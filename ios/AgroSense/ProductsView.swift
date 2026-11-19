import SwiftUI
import SwiftData

struct ProductsView: View {
    @Environment(\.modelContext) private var ctx
    @Query(sort: [SortDescriptor(\Product.name), SortDescriptor(\Product.crop)]) private var products: [Product]
    @State private var q = ""
    @State private var cat = ""
    @State private var showNew = false
    @State private var editing: Product? = nil

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 10) {
                Text("Usos autorizados según etiqueta: producto + cultivo + plaga + dosis.").foregroundStyle(Color.agMuted)
                HStack(spacing: 8) {
                    RgbButton(title: "Agregar", systemImage: "plus", colors: Rgb.orange) { showNew = true }
                    RgbButton(title: "Restaurar catálogo", systemImage: "arrow.clockwise", colors: Rgb.teal) { Seeder.insertCatalog(ctx, skipExisting: true) }
                }
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 6) {
                        chip("Todos", cat.isEmpty) { cat = "" }
                        ForEach(Catalog.categories, id: \.key) { c in chip("\(c.emoji) \(c.name)", cat == c.key) { cat = c.key } }
                    }
                }
                let list = products.filter { p in
                    (cat.isEmpty || p.category == cat) &&
                    (q.isEmpty || [p.name, p.active, p.crop, p.target].joined(separator: " ").localizedCaseInsensitiveContains(q))
                }
                if list.isEmpty { HelpBox(text: "No hay productos con ese filtro. Toca \"Agregar\" y captura los datos exactos de la etiqueta.") }
                ForEach(list) { p in
                    let c = Catalog.category(p.category)
                    ChoiceRow(title: "\(c.emoji) \(p.name)",
                              subtitle: "\(p.active.isEmpty ? "—" : p.active) · \(p.crop.isEmpty ? "cualquier cultivo" : p.crop.short) · \(p.target.isEmpty ? c.name : p.target)"
                                + (p.phiDays.map { " · IS \($0) d" } ?? "") + (p.source == "Catálogo" ? " · catálogo" : ""),
                              trailing: doseRange(p)) {
                        editing = p
                    }
                }
            }.padding(16)
        }
        .background(Color.agBg)
        .navigationTitle("🧴 Productos")
        .searchable(text: $q, prompt: "Producto, ingrediente, cultivo o plaga")
        .sheet(isPresented: $showNew) { ProductForm(product: nil, defaultCategory: cat.isEmpty ? "insecticida" : cat) { _ in } }
        .sheet(item: $editing) { p in ProductForm(product: p) { _ in } }
    }

    private func chip(_ t: String, _ on: Bool, _ a: @escaping () -> Void) -> some View {
        Button(action: a) {
            Text(t).font(.footnote.weight(on ? .bold : .regular)).padding(.horizontal, 12).padding(.vertical, 7)
                .background(on ? Color.agGreenSoft : .white).foregroundStyle(on ? Color.agGreen : .primary)
                .clipShape(Capsule()).overlay(Capsule().stroke(on ? Color.agGreen : Color.agLine))
        }.buttonStyle(.plain)
    }
}

struct ProductForm: View {
    let product: Product?
    var defaultCategory = "insecticida"
    var defaultCrop = ""
    var onSaved: (Int64) -> Void
    @Environment(\.modelContext) private var ctx
    @Environment(\.dismiss) private var dismiss
    @State private var name = "", category = "insecticida", active = "", formulation = "", registration = ""
    @State private var crop = "", target = "", doseMin = "", doseMax = "", unit = "L/ha", method = "", tox = ""
    @State private var phi = "", rei = "", maxApps = "", interval = "", price = "", priceUnit = "L"
    @State private var confirmDelete = false

    private func d(_ s: String) -> Double? { Double(s.replacingOccurrences(of: ",", with: ".")) }
    private var lo: Double { d(doseMin) ?? 0 }
    private var hi: Double { d(doseMax).flatMap { $0 > 0 ? $0 : nil } ?? lo }
    private var valid: Bool { !name.trimmingCharacters(in: .whitespaces).isEmpty && lo > 0 && hi >= lo }

    var body: some View {
        NavigationStack {
            Form {
                Section { HelpBox(text: "Copia los datos tal como aparecen en la etiqueta autorizada.") }
                Section("Producto") {
                    TextField("Nombre comercial *", text: $name)
                    Picker("Tipo *", selection: $category) { ForEach(Catalog.categories, id: \.key) { Text("\($0.emoji) \($0.name)").tag($0.key) } }
                    TextField("Ingrediente activo y concentración", text: $active)
                    TextField("Formulación (EC, SC, WG, WP, SL…)", text: $formulation)
                    TextField("Número de registro", text: $registration)
                }
                Section("Uso autorizado") {
                    Picker("Cultivo", selection: $crop) {
                        Text("Cualquiera (según etiqueta)").tag("")
                        ForEach(Catalog.crops, id: \.name) { Text("\($0.emoji) \($0.name)").tag($0.name) }
                    }
                    TextField("Plaga / maleza / enfermedad", text: $target)
                    HStack { TextField("Dosis mínima *", text: $doseMin).keyboardType(.decimalPad); TextField("Dosis máxima", text: $doseMax).keyboardType(.decimalPad) }
                    Picker("Unidad de dosis *", selection: $unit) { ForEach(Catalog.units, id: \.self) { Text($0).tag($0) } }
                    TextField("Forma de aplicación / notas de dosis", text: $method)
                }
                Section("Seguridad") {
                    Picker("Banda de color de la etiqueta", selection: $tox) { ForEach(["", "Rojo", "Amarillo", "Azul", "Verde"], id: \.self) { Text($0.isEmpty ? "Sin dato" : $0).tag($0) } }
                    HStack { Text("Intervalo de seguridad"); TextField("días", text: $phi).keyboardType(.numberPad).multilineTextAlignment(.trailing) }
                    HStack { Text("Reentrada"); TextField("horas", text: $rei).keyboardType(.decimalPad).multilineTextAlignment(.trailing) }
                    HStack { Text("Máx. aplicaciones/ciclo"); TextField("—", text: $maxApps).keyboardType(.numberPad).multilineTextAlignment(.trailing) }
                    HStack { Text("Intervalo mínimo"); TextField("días", text: $interval).keyboardType(.numberPad).multilineTextAlignment(.trailing) }
                }
                Section("Costo") {
                    HStack {
                        TextField("Precio MXN", text: $price).keyboardType(.decimalPad)
                        Picker("por", selection: $priceUnit) { ForEach(["L", "mL", "kg", "g"], id: \.self) { Text($0).tag($0) } }
                    }
                }
                if product != nil { Section { Button("Eliminar producto", role: .destructive) { confirmDelete = true } } }
                if !valid { Text("Completa nombre y dosis (la máxima no puede ser menor que la mínima).").font(.caption).foregroundStyle(Color.agDanger) }
            }
            .navigationTitle(product == nil ? "Nuevo producto" : "Editar producto")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancelar") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) { Button("Guardar") { save() }.disabled(!valid) }
            }
            .alert("¿Eliminar producto?", isPresented: $confirmDelete) {
                Button("Eliminar", role: .destructive) { if let product { ctx.delete(product); try? ctx.save() }; dismiss() }
                Button("Cancelar", role: .cancel) {}
            } message: { Text("Las aplicaciones ya registradas conservan sus datos.") }
            .onAppear(perform: load)
        }
    }

    private func load() {
        guard let p = product else { category = defaultCategory; crop = defaultCrop; return }
        name = p.name; category = p.category; active = p.active; formulation = p.formulation; registration = p.registration
        crop = p.crop; target = p.target; doseMin = Calculator.plain(p.doseMin); doseMax = Calculator.plain(p.doseMax); unit = p.unit
        method = p.method; tox = p.toxBand; phi = p.phiDays.map(String.init) ?? ""; rei = p.reiHours.map { Calculator.plain($0) } ?? ""
        maxApps = p.maxApps.map(String.init) ?? ""; interval = p.intervalDays.map(String.init) ?? ""
        price = p.price.map { Calculator.plain($0, 2) } ?? ""; priceUnit = p.priceUnit
    }

    private func save() {
        let p = product ?? Product(id: IdGen.next(Product.self, ctx) { $0.id }, name: "", category: category, doseMin: lo, doseMax: hi, unit: unit)
        p.name = name.trimmingCharacters(in: .whitespaces); p.category = category; p.active = active; p.formulation = formulation
        p.registration = registration; p.crop = crop; p.target = target; p.doseMin = lo; p.doseMax = hi; p.unit = unit; p.method = method
        p.toxBand = tox; p.phiDays = Int(phi); p.reiHours = d(rei); p.maxApps = Int(maxApps); p.intervalDays = Int(interval)
        p.price = d(price); p.priceUnit = priceUnit
        if product == nil { ctx.insert(p) }
        try? ctx.save()
        onSaved(p.id)
        dismiss()
    }
}
