import SwiftUI
import SwiftData

struct WizardView: View {
    let startClient: Int64?
    let startPlot: Int64?

    @Environment(\.modelContext) private var ctx
    @Environment(Router.self) private var router
    @Query(sort: \Client.name) private var clients: [Client]
    @Query(sort: \Plot.name) private var plots: [Plot]
    @Query(sort: \Product.name) private var products: [Product]
    @Query private var history: [ApplicationRecord]

    private let steps = ["Cliente", "Cultivo", "Producto", "Área", "Resultado"]
    @State private var step = 0
    @State private var clientId: Int64? = nil
    @State private var plotId: Int64? = nil
    @State private var crop = ""
    @State private var cropQuery = ""
    @State private var category = ""
    @State private var productId: Int64? = nil
    @State private var target = ""
    @State private var areaText = ""
    @State private var areaUnit = "m²"
    @State private var equipKey = ""
    @State private var waterText = "", tankText = "", flowText = "", refillText = ""
    @State private var showAdvanced = false
    @State private var doseText = ""
    @State private var priceText = ""
    @State private var appDate = Date()
    @State private var operatorName = "", weather = "", notes = ""
    @State private var ppeChecked: Set<String> = []
    @State private var showNewClient = false, showNewPlot = false, showNewProduct = false
    @State private var saved = false

    private var client: Client? { clients.first { $0.id == clientId } }
    private var plot: Plot? { plots.first { $0.id == plotId } }
    private var product: Product? { products.first { $0.id == productId } }
    private var cropInfo: CropInfo? { Catalog.crop(crop) }
    private var equip: EquipmentInfo? { equipKey.isEmpty ? nil : Catalog.equipment(equipKey) }
    private func num(_ s: String) -> Double { Double(s.replacingOccurrences(of: ",", with: ".")) ?? 0 }
    private var areaHa: Double { areaUnit == "m²" ? num(areaText) / 10_000 : num(areaText) }
    private var dose: Double { num(doseText) }

    private var outcome: Calculator.Outcome? {
        guard let p = product else { return nil }
        return Calculator.calculate(.init(dose: dose, unit: p.unit, areaHa: areaHa, waterLHa: num(waterText), tankL: num(tankText),
                                          flowLMin: num(flowText), refillMin: num(refillText), price: num(priceText).nonZero, priceUnit: p.priceUnit))
    }
    private var result: Calculator.Result? { if case .ok(let r) = outcome { return r }; return nil }

    private var canNext: Bool {
        switch step {
        case 0: return true
        case 1: return !crop.isEmpty
        case 2: return product != nil
        case 3: return result != nil && equip != nil
        default: return false
        }
    }

    var body: some View {
        VStack(spacing: 0) {
            StepIndicator(current: step, labels: steps).padding(.horizontal).padding(.vertical, 8)
            ScrollViewReader { proxy in
                ScrollView {
                    VStack(alignment: .leading, spacing: 12) {
                        Color.clear.frame(height: 0).id("top")
                        switch step {
                        case 0: stepClient
                        case 1: stepCrop
                        case 2: stepProduct
                        case 3: stepArea
                        default: stepResult
                        }
                    }.padding(16)
                }
                .onChange(of: step) { _, _ in withAnimation { proxy.scrollTo("top") } }
            }
            HStack(spacing: 10) {
                if step > 0 { RgbButton(title: "Atrás", systemImage: "chevron.left", colors: Rgb.purple) { step -= 1 } }
                if step < 4 {
                    RgbButton(title: step == 3 ? "Ver resultado" : "Siguiente", systemImage: "chevron.right", colors: canNext ? Rgb.green : Rgb.gray, enabled: canNext) {
                        if step == 2 && equipKey.isEmpty, let c = cropInfo { selectEquipment(c.equipment) }
                        step += 1
                    }
                }
            }.padding(12).background(.bar)
        }
        .background(Color.agBg)
        .navigationTitle("🧮 Nueva aplicación")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear {
            clientId = startClient
            if let sp = startPlot { selectPlot(sp) }
        }
        .sheet(isPresented: $showNewClient) { ClientForm(client: nil) { id in clientId = id; plotId = nil } }
        .sheet(isPresented: $showNewPlot) { if let c = client { PlotForm(clientId: c.id, plot: nil, defaultCrop: crop) { id in selectPlot(id) } } }
        .sheet(isPresented: $showNewProduct) { ProductForm(product: nil, defaultCategory: category, defaultCrop: crop) { id in pickProduct(id) } }
        .alert("Aplicación guardada ✅", isPresented: $saved) {
            Button("Ver historial") { router.newApplication(); router.tab = 4 }
            Button("Nueva aplicación") { router.newApplication() }
        }
    }

    // MARK: acciones
    private func selectPlot(_ id: Int64) {
        plotId = id
        if let p = plots.first(where: { $0.id == id }) ?? (try? ctx.fetch(FetchDescriptor<Plot>()))?.first(where: { $0.id == id }) {
            crop = p.crop; areaText = Calculator.plain(p.areaHa); areaUnit = "ha"
        }
    }
    private func pickProduct(_ id: Int64) {
        productId = id
        guard let p = products.first(where: { $0.id == id }) ?? (try? ctx.fetch(FetchDescriptor<Product>()))?.first(where: { $0.id == id }) else { return }
        doseText = Calculator.plain(p.doseMin)
        target = p.target
        priceText = p.price.map { Calculator.plain($0, 2) } ?? ""
    }
    private func selectEquipment(_ key: String) {
        let e = Catalog.equipment(key)
        equipKey = key
        tankText = Calculator.plain(e.tankL, 1); flowText = Calculator.plain(e.flowLMin, 2); refillText = Calculator.plain(e.refillMin, 1)
        if e.fixedWaterLHa > 0 { waterText = Calculator.plain(e.fixedWaterLHa, 0) }
        else if let c = cropInfo { waterText = "\((c.waterMin + c.waterMax) / 2)" }
        else { waterText = "300" }
    }

    // MARK: Paso 1
    private var stepClient: some View {
        VStack(alignment: .leading, spacing: 12) {
            HelpBox(text: "Paso 1 de 5 · Elige para quién es la aplicación y en qué parcela. Si solo quieres calcular, deja \"Sin cliente\" y toca Siguiente.")
            SectionCard(title: "Cliente", emoji: "👨‍🌾") {
                Picker("Cliente", selection: Binding(get: { clientId }, set: { clientId = $0; plotId = nil })) {
                    Text("Sin cliente (solo calcular)").tag(Int64?.none)
                    ForEach(clients) { Text($0.name).tag(Int64?.some($0.id)) }
                }.pickerStyle(.menu)
                RgbButton(title: "Cliente nuevo", systemImage: "plus", colors: Rgb.blue, height: 46) { showNewClient = true }
            }
            if let c = client {
                SectionCard(title: "Parcela", emoji: "🗺️") {
                    let cp = plots.filter { $0.clientId == c.id }
                    Picker("Parcela / lote", selection: Binding(get: { plotId }, set: { if let v = $0 { selectPlot(v) } else { plotId = nil } })) {
                        Text("Sin parcela").tag(Int64?.none)
                        ForEach(cp) { p in Text("\(Catalog.crop(p.crop)?.emoji ?? "") \(p.name) · \(Calculator.fmt(p.areaHa)) ha").tag(Int64?.some(p.id)) }
                    }.pickerStyle(.menu)
                    RgbButton(title: "Parcela nueva", systemImage: "plus", colors: Rgb.teal, height: 46) { showNewPlot = true }
                }
            }
        }
    }

    // MARK: Paso 2
    private var stepCrop: some View {
        VStack(alignment: .leading, spacing: 10) {
            HelpBox(text: "Paso 2 de 5 · Toca el cultivo. El cultivo define el volumen de agua recomendado y los productos autorizados.")
            TextField("🔎 Buscar cultivo…", text: $cropQuery).padding(12).background(.white).clipShape(RoundedRectangle(cornerRadius: 12))
            let filtered = Catalog.crops.filter { cropQuery.isEmpty || $0.name.localizedCaseInsensitiveContains(cropQuery) }
            let groups = Dictionary(grouping: filtered, by: \.group)
            ForEach(["Granos básicos", "Industriales y oleaginosas", "Hortalizas", "Berries y vid", "Frutales"], id: \.self) { g in
                if let list = groups[g] {
                    Text(g).font(.headline).foregroundStyle(Color.agGreen)
                    LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 8), count: 3), spacing: 8) {
                        ForEach(list, id: \.name) { c in
                            ChoiceCard(emoji: c.emoji, title: c.name.short, subtitle: "\(c.waterMin)–\(c.waterMax) L/ha", selected: crop == c.name) {
                                crop = c.name; productId = nil; equipKey = ""
                            }
                        }
                    }
                }
            }
        }
    }

    // MARK: Paso 3
    private var stepProduct: some View {
        VStack(alignment: .leading, spacing: 12) {
            HelpBox(text: "Paso 3 de 5 · ¿Qué quieres controlar? Elige el tipo y luego el producto. Primero aparecen los autorizados para \(crop.short).")
            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 8), count: 3), spacing: 8) {
                ForEach(Catalog.categories, id: \.key) { c in
                    ChoiceCard(emoji: c.emoji, title: c.name, subtitle: c.controls, selected: category == c.key, accent: Color(hex: c.color)) {
                        category = c.key; productId = nil; target = ""
                    }
                }
            }
            if !category.isEmpty {
                let cat = Catalog.category(category)
                if cat.restricted { HelpBox(text: "\(cat.name): uso restringido. Solo con autorización vigente y conforme a la etiqueta.", warn: true) }
                let inCat = products.filter { $0.category == category }
                let forCrop = inCat.filter { $0.crop == crop }
                let others = inCat.filter { $0.crop != crop }
                SectionCard(title: "Productos para \(crop.short)", emoji: cat.emoji) {
                    if forCrop.isEmpty { Text("No hay \(cat.name.lowercased()) registrados para este cultivo. Agrega uno con los datos de su etiqueta.").font(.subheadline).foregroundStyle(Color.agMuted) }
                    ForEach(forCrop) { p in productRow(p) }
                }
                if !others.isEmpty {
                    SectionCard(title: "Otros \(cat.name.lowercased()) del catálogo", emoji: "📦") {
                        Text("Verifica que la etiqueta autorice su uso en tu cultivo antes de elegirlos.").font(.footnote).foregroundStyle(Color.agDanger)
                        ForEach(others) { p in productRow(p) }
                    }
                }
                RgbButton(title: "Agregar producto de etiqueta", systemImage: "plus", colors: Rgb.orange, height: 46) { showNewProduct = true }
                if product != nil {
                    TextField("Plaga / maleza / enfermedad a controlar", text: $target).padding(12).background(.white).clipShape(RoundedRectangle(cornerRadius: 12))
                }
            }
        }
    }

    private func productRow(_ p: Product) -> some View {
        ChoiceRow(title: p.name,
                  subtitle: "\(p.active.isEmpty ? "—" : p.active) · \(p.target.isEmpty ? "uso general" : p.target)\(p.crop.isEmpty ? "" : " · " + p.crop.short)",
                  trailing: doseRange(p), selected: productId == p.id) { pickProduct(p.id) }
    }

    // MARK: Paso 4
    @ViewBuilder private var stepArea: some View {
        if let p = product {
            VStack(alignment: .leading, spacing: 12) {
                HelpBox(text: "Paso 4 de 5 · Escribe la superficie, elige tu equipo y ajusta la dosis dentro del rango de la etiqueta.")
                SectionCard(title: "Superficie a tratar", emoji: "📐") {
                    NumberField(label: "Superficie", text: $areaText)
                    Picker("Unidad", selection: $areaUnit) { Text("m²").tag("m²"); Text("ha").tag("ha") }.pickerStyle(.segmented)
                    if areaHa > 0 { Text("= \(Calculator.fmt(areaHa, 4)) ha · \(Calculator.fmt(areaHa * 10_000, 0)) m²").font(.footnote).foregroundStyle(Color.agMuted) }
                }
                SectionCard(title: "Equipo de aplicación", emoji: "🧰") {
                    LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 8), count: 3), spacing: 8) {
                        ForEach(Catalog.equipment, id: \.key) { e in
                            ChoiceCard(emoji: e.emoji, title: e.name, subtitle: cropInfo?.equipment == e.key ? "⭐ Sugerido" : "\(Calculator.fmt(e.tankL, 0)) L",
                                       selected: equipKey == e.key, accent: Color(hex: 0x2979FF)) { selectEquipment(e.key) }
                        }
                    }
                }
                if let e = equip {
                    SectionCard(title: "Agua y tanque", emoji: "💧") {
                        NumberField(label: "Volumen de agua por hectárea", text: $waterText, suffix: "L/ha")
                        Text(e.fixedWaterLHa > 0 ? "Dron: usa el volumen que indique la etiqueta para aplicación aérea."
                             : "Rango orientativo para \(crop.short): \(cropInfo?.waterMin ?? 0)–\(cropInfo?.waterMax ?? 0) L/ha. Usa el de tu calibración.")
                            .font(.caption).foregroundStyle(Color.agMuted)
                        NumberField(label: "Capacidad del tanque", text: $tankText, suffix: "L")
                        DisclosureGroup("Ajustes del equipo (gasto y recarga)", isExpanded: $showAdvanced) {
                            NumberField(label: "Gasto del equipo (mide los litros que descarga en 1 minuto)", text: $flowText, suffix: "L/min")
                            NumberField(label: "Tiempo de recarga por tanque", text: $refillText, suffix: "min")
                        }
                    }
                }
                SectionCard(title: "Dosis de etiqueta", emoji: "🧪") {
                    Text("\(p.name) · \(p.active)").fontWeight(.semibold)
                    Text("Rango de etiqueta: \(doseRange(p))\(p.method.isEmpty ? "" : " · " + p.method)").font(.footnote).foregroundStyle(Color.agMuted)
                    if p.doseMax > p.doseMin {
                        Slider(value: Binding(get: { min(max(dose, p.doseMin), p.doseMax) }, set: { doseText = Calculator.plain($0, 3) }), in: p.doseMin...p.doseMax)
                        HStack { Text("Mínima"); Spacer(); Text("Máxima") }.font(.caption).foregroundStyle(Color.agMuted)
                        Text("Usa la dosis baja con poca presión de plaga y la alta con mucha presión.").font(.caption).foregroundStyle(Color.agMuted)
                    }
                    NumberField(label: "Dosis a usar", text: $doseText, suffix: p.unit)
                    if p.unit.hasPrefix("%") { Text("El porcentaje se calcula sobre el caldo: 1 % = 10 \(p.unit == "% v/v" ? "mL" : "g") por litro de agua.").font(.caption).foregroundStyle(Color.agMuted) }
                    NumberField(label: "Precio del producto (opcional)", text: $priceText, suffix: "MXN/\(p.priceUnit)")
                }
                if equip != nil, case .error(let msg) = outcome { HelpBox(text: msg, warn: true) }
            }
        }
    }

    // MARK: Paso 5
    @ViewBuilder private var stepResult: some View {
        if let p = product, let r = result, let e = equip {
            let warnings = Safety.warnings(product: p, plotId: plot?.id, history: history, date: appDate, dose: dose)
            let ppe = Safety.ppe(category: p.category, formulation: p.formulation, toxBand: p.toxBand, equipment: e.key)
            VStack(alignment: .leading, spacing: 12) {
                HelpBox(text: "Paso 5 de 5 · Revisa el resultado, marca el equipo de protección y guarda la aplicación.")
                ForEach(warnings, id: \.self) { HelpBox(text: $0, warn: true) }
                SectionCard(title: "Lo que necesitas", emoji: "✅") {
                    Text(p.name).foregroundStyle(Color.agMuted)
                    Text(Calculator.fmtQty(r.productBase, r.baseUnit)).font(.system(size: 34, weight: .heavy)).foregroundStyle(Color.agGreen)
                    Text("de producto para \(Calculator.fmt(areaHa, 4)) ha de \(crop.short)").foregroundStyle(Color.agMuted)
                    if !p.method.isEmpty { Text("Nota de etiqueta: \(p.method)").font(.footnote).foregroundStyle(Color.agWarn) }
                }
                HStack(spacing: 10) {
                    StatCard(label: "Agua total", value: "\(Calculator.fmt(r.waterTotalL, 1)) L", emoji: "💧", color: Color(hex: 0x2979FF))
                    StatCard(label: "Tanques", value: "\(r.tanksTotal)", emoji: e.emoji, color: Color(hex: 0x7C4DFF))
                }
                HStack(spacing: 10) {
                    StatCard(label: "Tiempo estimado", value: Calculator.fmtMinutes(r.totalMin), emoji: "⏱️", color: Color(hex: 0xFF6D00))
                    StatCard(label: "Costo", value: money(r.cost), emoji: "💲")
                }
                SectionCard(title: "Preparación por tanque", emoji: "🪣") {
                    if r.fullTanks > 0 { Text("• \(r.fullTanks) tanque(s) llenos de \(Calculator.fmt(num(tankText), 1)) L → \(Calculator.fmtQty(r.productPerFullTank, r.baseUnit)) de producto cada uno") }
                    if r.lastTankL > 0 { Text("• Último tanque con \(Calculator.fmt(r.lastTankL, 1)) L de agua → \(Calculator.fmtQty(r.productLastTank, r.baseUnit)) de producto") }
                    Text("Concentración: \(Calculator.fmt(r.productPerLiter, 3)) \(r.baseUnit) por litro de agua.").font(.footnote).foregroundStyle(Color.agMuted)
                    Text("Tiempo: \(Calculator.fmtMinutes(r.sprayMin)) aplicando + \(Calculator.fmtMinutes(r.refillTotalMin)) de recargas (estimado con el gasto del equipo).").font(.footnote).foregroundStyle(Color.agMuted)
                }
                SectionCard(title: "Cuándo aplicar", emoji: "🌤️") { ForEach(Safety.bestTime, id: \.self) { Text($0).font(.subheadline) } }
                SectionCard(title: "Antes de aplicar", emoji: "📋") { ForEach(Safety.beforeApplying(e.key), id: \.self) { Text("• \($0)").font(.subheadline) } }
                SectionCard(title: "Equipo de protección (márcalo)", emoji: "🦺") {
                    ForEach(ppe, id: \.self) { item in
                        Button {
                            if ppeChecked.contains(item) { ppeChecked.remove(item) } else { ppeChecked.insert(item) }
                        } label: {
                            HStack(alignment: .top) {
                                Image(systemName: ppeChecked.contains(item) ? "checkmark.square.fill" : "square").foregroundStyle(Color.agGreen)
                                Text(item).font(.subheadline).foregroundStyle(.primary)
                            }
                        }.buttonStyle(.plain)
                    }
                    let missing = ppe.filter { !ppeChecked.contains($0) }.count
                    if missing > 0 { Text("Faltan \(missing) elementos por confirmar.").font(.caption).foregroundStyle(Color.agDanger) }
                }
                SectionCard(title: "Después de aplicar", emoji: "🧼") {
                    if let rei = p.reiHours { Text("🚷 Reentrada: \(Calculator.fmt(rei)) h → \(dateTimeFmt.string(from: appDate.addingTimeInterval(rei * 3600)))").fontWeight(.semibold) }
                    if let phi = p.phiDays { Text("🧺 Cosechar a partir de: \(dateFmt.string(from: appDate.addingTimeInterval(Double(phi) * 86_400))) (\(phi) días)").fontWeight(.semibold) }
                    if p.reiHours == nil || p.phiDays == nil { Text("Captura la reentrada y el intervalo de seguridad en el producto para calcular las fechas.").font(.caption).foregroundStyle(Color.agMuted) }
                    ForEach(Safety.afterApplying(p.category), id: \.self) { Text("• \($0)").font(.subheadline) }
                }
                SectionCard(title: "Datos del registro", emoji: "📝") {
                    Text("Cliente: \(client?.name ?? "Sin cliente") · Parcela: \(plot?.name ?? "—")").font(.subheadline)
                    DatePicker("Fecha y hora", selection: $appDate)
                    TextField("Responsable / aplicador", text: $operatorName).textFieldStyle(.roundedBorder)
                    TextField("Clima (viento, temperatura, humedad)", text: $weather).textFieldStyle(.roundedBorder)
                    TextField("Observaciones", text: $notes, axis: .vertical).lineLimit(3...6).textFieldStyle(.roundedBorder)
                }
                RgbButton(title: "Guardar aplicación", systemImage: "square.and.arrow.down.fill", colors: Rgb.green, height: 60) { save(p, r, e) }
            }
        }
    }

    private func save(_ p: Product, _ r: Calculator.Result, _ e: EquipmentInfo) {
        let rec = ApplicationRecord(
            id: IdGen.next(ApplicationRecord.self, ctx) { $0.id }, clientId: client?.id, plotId: plot?.id,
            clientName: client?.name ?? "", plotName: plot?.name ?? "", crop: crop, category: p.category, productId: p.id,
            productName: p.name, active: p.active, target: target, date: appDate, areaHa: areaHa, dose: dose, doseUnit: p.unit,
            productTotal: r.productBase, productBaseUnit: r.baseUnit, waterTotalL: r.waterTotalL, waterLHa: num(waterText).nonZero,
            tanks: r.tanksTotal, tankL: num(tankText).nonZero, equipment: e.name, durationMin: r.totalMin, cost: r.cost,
            operatorName: operatorName, weather: weather, ppe: ppeChecked.sorted().joined(separator: "|"), phiDays: p.phiDays,
            reiHours: p.reiHours, harvestAllowedAt: p.phiDays.map { appDate.addingTimeInterval(Double($0) * 86_400) },
            reentryAt: p.reiHours.map { appDate.addingTimeInterval($0 * 3600) }, notes: notes)
        ctx.insert(rec)
        if let pr = num(priceText).nonZero, pr != p.price { p.price = pr }   // recuerda el precio
        try? ctx.save()
        saved = true
    }
}

func doseRange(_ p: Product) -> String {
    "\(Calculator.fmt(p.doseMin, 3))\(p.doseMax > p.doseMin ? "–" + Calculator.fmt(p.doseMax, 3) : "") \(p.unit)"
}
