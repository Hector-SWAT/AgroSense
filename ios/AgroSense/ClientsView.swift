import SwiftUI
import SwiftData

struct ClientsView: View {
    @Query(sort: \Client.name) private var clients: [Client]
    @Query private var plots: [Plot]
    @Query private var apps: [ApplicationRecord]
    @State private var q = ""
    @State private var showNew = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 10) {
                Text("Cada cliente tiene sus parcelas y el historial de lo aplicado.").foregroundStyle(Color.agMuted)
                RgbButton(title: "Agregar cliente", systemImage: "plus", colors: Rgb.blue) { showNew = true }
                let list = clients.filter { q.isEmpty || ($0.name + " " + $0.location).localizedCaseInsensitiveContains(q) }
                if list.isEmpty { HelpBox(text: clients.isEmpty ? "Aún no tienes clientes. Toca \"Agregar cliente\" para registrar al primero." : "Sin resultados.") }
                ForEach(list) { c in
                    let cp = plots.filter { $0.clientId == c.id }
                    NavigationLink { ClientDetailView(client: c) } label: {
                        SectionCard {
                            HStack(spacing: 12) {
                                Text(String(c.name.prefix(1)).uppercased()).font(.title2.bold()).foregroundStyle(.white)
                                    .frame(width: 46, height: 46).background(LinearGradient(colors: Rgb.teal, startPoint: .top, endPoint: .bottom)).clipShape(Circle())
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(c.name).font(.headline)
                                    Text([c.location, c.phone].filter { !$0.isEmpty }.joined(separator: " · ")).font(.caption).foregroundStyle(Color.agMuted)
                                    Text("\(cp.count) parcelas · \(apps.filter { $0.clientId == c.id }.count) aplicaciones · \(Calculator.fmt(cp.reduce(0) { $0 + $1.areaHa })) ha")
                                        .font(.caption).foregroundStyle(Color.agGreen)
                                }
                                Spacer()
                                Image(systemName: "chevron.right").foregroundStyle(Color.agMuted)
                            }
                        }
                    }.buttonStyle(.plain)
                }
            }.padding(16)
        }
        .background(Color.agBg)
        .navigationTitle("👨‍🌾 Clientes")
        .searchable(text: $q, prompt: "Buscar por nombre o comunidad")
        .sheet(isPresented: $showNew) { ClientForm(client: nil) { _ in } }
    }
}

struct ClientForm: View {
    let client: Client?
    var onSaved: (Int64) -> Void
    @Environment(\.modelContext) private var ctx
    @Environment(\.dismiss) private var dismiss
    @State private var name = "", phone = "", location = "", notes = ""

    var body: some View {
        NavigationStack {
            Form {
                TextField("Nombre del productor *", text: $name)
                TextField("Teléfono", text: $phone).keyboardType(.phonePad)
                TextField("Comunidad / municipio", text: $location)
                TextField("Notas", text: $notes, axis: .vertical).lineLimit(3...6)
            }
            .navigationTitle(client == nil ? "Nuevo cliente" : "Editar cliente")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancelar") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Guardar") {
                        let n = name.trimmingCharacters(in: .whitespaces)
                        if let client {
                            client.name = n; client.phone = phone; client.location = location; client.notes = notes
                            onSaved(client.id)
                        } else {
                            let c = Client(id: IdGen.next(Client.self, ctx) { $0.id }, name: n, phone: phone, location: location, notes: notes)
                            ctx.insert(c); onSaved(c.id)
                        }
                        try? ctx.save(); dismiss()
                    }.disabled(name.trimmingCharacters(in: .whitespaces).isEmpty)
                }
            }
            .onAppear { if let client { name = client.name; phone = client.phone; location = client.location; notes = client.notes } }
        }
    }
}

struct PlotForm: View {
    let clientId: Int64
    let plot: Plot?
    var defaultCrop: String = ""
    var onSaved: (Int64) -> Void
    @Environment(\.modelContext) private var ctx
    @Environment(\.dismiss) private var dismiss
    @State private var name = "", crop = Catalog.crops[0].name, variety = "", area = ""
    @State private var hasStart = false, hasEnd = false
    @State private var start = Date(), end = Date().addingTimeInterval(120 * 86_400)

    var body: some View {
        NavigationStack {
            Form {
                TextField("Nombre de la parcela / lote *", text: $name)
                Picker("Cultivo *", selection: $crop) { ForEach(Catalog.crops, id: \.name) { Text("\($0.emoji) \($0.name)").tag($0.name) } }
                TextField("Variedad", text: $variety)
                HStack { TextField("Superficie *", text: $area).keyboardType(.decimalPad); Text("ha").foregroundStyle(Color.agMuted) }
                Section("Ciclo de cultivo") {
                    Toggle("Fecha de siembra", isOn: $hasStart)
                    if hasStart { DatePicker("Siembra", selection: $start, displayedComponents: .date) }
                    Toggle("Cosecha estimada", isOn: $hasEnd)
                    if hasEnd { DatePicker("Cosecha", selection: $end, displayedComponents: .date) }
                }
            }
            .navigationTitle(plot == nil ? "Nueva parcela" : "Editar parcela")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancelar") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Guardar") {
                        let a = Double(area.replacingOccurrences(of: ",", with: ".")) ?? 0
                        if let plot {
                            plot.name = name; plot.crop = crop; plot.variety = variety; plot.areaHa = a
                            plot.cycleStart = hasStart ? start : nil; plot.cycleEnd = hasEnd ? end : nil
                            onSaved(plot.id)
                        } else {
                            let p = Plot(id: IdGen.next(Plot.self, ctx) { $0.id }, clientId: clientId, name: name, crop: crop, variety: variety,
                                         areaHa: a, cycleStart: hasStart ? start : nil, cycleEnd: hasEnd ? end : nil)
                            ctx.insert(p); onSaved(p.id)
                        }
                        try? ctx.save(); dismiss()
                    }.disabled(name.isEmpty || (Double(area.replacingOccurrences(of: ",", with: ".")) ?? 0) <= 0)
                }
            }
            .onAppear {
                if let plot {
                    name = plot.name; crop = plot.crop; variety = plot.variety; area = Calculator.plain(plot.areaHa)
                    if let s = plot.cycleStart { hasStart = true; start = s }
                    if let e = plot.cycleEnd { hasEnd = true; end = e }
                } else if !defaultCrop.isEmpty { crop = defaultCrop }
            }
        }
    }
}

struct ClientDetailView: View {
    @Bindable var client: Client
    @Environment(\.modelContext) private var ctx
    @Environment(\.dismiss) private var dismiss
    @Environment(Router.self) private var router
    @Query private var allPlots: [Plot]
    @Query(sort: \ApplicationRecord.date) private var allApps: [ApplicationRecord]
    @State private var editClient = false
    @State private var editingPlot: Plot? = nil
    @State private var newPlot = false
    @State private var confirmDelete = false

    var body: some View {
        let plots = allPlots.filter { $0.clientId == client.id }
        let apps = allApps.filter { $0.clientId == client.id }
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                Text([client.location, client.phone].filter { !$0.isEmpty }.joined(separator: " · ")).foregroundStyle(Color.agMuted)
                HStack(spacing: 10) {
                    StatCard(label: "Aplicaciones", value: "\(apps.count)", emoji: "🧪")
                    StatCard(label: "Costo total", value: money(apps.reduce(0) { $0 + ($1.cost ?? 0) }), emoji: "💲")
                }
                RgbButton(title: "Agregar parcela", systemImage: "plus", colors: Rgb.teal) { newPlot = true }
                if plots.isEmpty { HelpBox(text: "Agrega las parcelas del cliente (nombre, cultivo y superficie). Así cada aplicación queda ligada a su ciclo de cultivo.") }
                ForEach(plots) { p in
                    PlotCard(plot: p, apps: apps.filter { $0.plotId == p.id },
                             onApply: { router.newApplication(client: client.id, plot: p.id) },
                             onEdit: { editingPlot = p })
                }
                let loose = apps.filter { a in !plots.contains { $0.id == a.plotId } }
                if !loose.isEmpty { SectionCard(title: "Aplicaciones sin parcela", emoji: "📄") { TimelineList(apps: loose) } }
                RgbButton(title: "Nueva aplicación para \(client.name)", systemImage: "drop.fill", colors: Rgb.green) { router.newApplication(client: client.id) }
            }.padding(16)
        }
        .background(Color.agBg)
        .navigationTitle(client.name)
        .toolbar {
            ToolbarItem { Button { editClient = true } label: { Image(systemName: "pencil") } }
            ToolbarItem { Button(role: .destructive) { confirmDelete = true } label: { Image(systemName: "trash") } }
        }
        .sheet(isPresented: $editClient) { ClientForm(client: client) { _ in } }
        .sheet(isPresented: $newPlot) { PlotForm(clientId: client.id, plot: nil) { _ in } }
        .sheet(item: $editingPlot) { p in PlotForm(clientId: client.id, plot: p) { _ in } }
        .alert("¿Eliminar cliente?", isPresented: $confirmDelete) {
            Button("Eliminar", role: .destructive) {
                for p in allPlots where p.clientId == client.id { ctx.delete(p) }
                ctx.delete(client); try? ctx.save(); dismiss()
            }
            Button("Cancelar", role: .cancel) {}
        } message: { Text("Se borrarán el cliente y sus parcelas. Las aplicaciones registradas se conservan en el historial.") }
    }
}

struct PlotCard: View {
    let plot: Plot
    let apps: [ApplicationRecord]
    let onApply: () -> Void
    let onEdit: () -> Void

    var body: some View {
        let inCycle = apps.filter { a in (plot.cycleStart.map { a.date >= $0 } ?? true) && (plot.cycleEnd.map { a.date <= $0.addingTimeInterval(86_400) } ?? true) }
        let harvest = apps.compactMap(\.harvestAllowedAt).max()
        SectionCard(title: "\(plot.name) · \(Calculator.fmt(plot.areaHa)) ha", emoji: Catalog.crop(plot.crop)?.emoji ?? "🌱") {
            Text(plot.crop + (plot.variety.isEmpty ? "" : " · \(plot.variety)")).foregroundStyle(Color.agMuted)
            if plot.cycleStart != nil || plot.cycleEnd != nil {
                Text("Ciclo: \(plot.cycleStart.map { dateFmt.string(from: $0) } ?? "?") → \(plot.cycleEnd.map { dateFmt.string(from: $0) } ?? "?")").font(.footnote)
            }
            Text("Aplicaciones en el ciclo: \(inCycle.count) · Costo: \(money(inCycle.reduce(0) { $0 + ($1.cost ?? 0) }))").font(.footnote).foregroundStyle(Color.agGreen)
            if let harvest {
                Text(harvest <= Date() ? "🧺 Cosecha permitida (intervalo de seguridad cumplido)" : "⏳ Cosechar a partir de \(dateFmt.string(from: harvest))")
                    .font(.footnote.weight(.semibold)).foregroundStyle(harvest <= Date() ? Color.agGreen : Color.agWarn)
            }
            let byActive = Dictionary(grouping: inCycle) { $0.active.isEmpty ? $0.productName : $0.active }
            if !byActive.isEmpty {
                Text("Ingredientes activos usados:").font(.footnote.weight(.semibold))
                ForEach(byActive.keys.sorted(), id: \.self) { k in Text("• \(k): \(byActive[k]!.count) vez/veces").font(.footnote) }
            }
            TimelineList(apps: apps)
            HStack(spacing: 8) {
                RgbButton(title: "Aplicar aquí", systemImage: "drop.fill", colors: Rgb.green, height: 46, action: onApply)
                RgbButton(title: "Editar", systemImage: "pencil", colors: Rgb.purple, height: 46, action: onEdit)
            }
        }
    }
}

struct TimelineList: View {
    let apps: [ApplicationRecord]
    var body: some View {
        if apps.isEmpty { Text("Sin aplicaciones registradas.").font(.footnote).foregroundStyle(Color.agMuted) }
        let sorted = apps.sorted { $0.date < $1.date }
        ForEach(Array(sorted.enumerated()), id: \.element.id) { i, a in
            let cat = Catalog.category(a.category)
            NavigationLink { ApplicationDetailView(app: a) } label: {
                HStack(alignment: .top, spacing: 10) {
                    VStack(spacing: 0) {
                        Text(cat.emoji).font(.system(size: 15)).frame(width: 30, height: 30).background(Color(hex: cat.color)).clipShape(Circle())
                        if i < sorted.count - 1 { Rectangle().fill(Color.agLine).frame(width: 2, height: 24) }
                    }
                    VStack(alignment: .leading, spacing: 2) {
                        Text("\(dateFmt.string(from: a.date)) · \(a.productName)").font(.subheadline.weight(.semibold))
                        Text("\(a.target.isEmpty ? cat.name : a.target) · \(Calculator.fmt(a.dose)) \(a.doseUnit) · \(Calculator.fmtQty(a.productTotal, a.productBaseUnit))")
                            .font(.caption).foregroundStyle(Color.agMuted)
                    }
                    Spacer()
                }
            }.buttonStyle(.plain)
        }
    }
}
