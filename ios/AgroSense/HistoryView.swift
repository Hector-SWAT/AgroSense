import SwiftUI
import SwiftData
import UniformTypeIdentifiers

struct HistoryView: View {
    @Query(sort: \ApplicationRecord.date, order: .reverse) private var apps: [ApplicationRecord]
    @State private var q = ""
    @State private var cat = ""
    @State private var useRange = false
    @State private var from = Calendar.current.date(byAdding: .month, value: -3, to: .now)!
    @State private var to = Date()

    private func matches(_ a: ApplicationRecord) -> Bool {
        if !q.isEmpty {
            let text = [a.clientName, a.plotName, a.crop, a.productName, a.target, a.operatorName].joined(separator: " ")
            if !text.localizedCaseInsensitiveContains(q) { return false }
        }
        if !cat.isEmpty && a.category != cat { return false }
        if useRange {
            let start = Calendar.current.startOfDay(for: from)
            let end = Calendar.current.startOfDay(for: to).addingTimeInterval(86_400)
            if a.date < start || a.date >= end { return false }
        }
        return true
    }

    var body: some View {
        let list = apps.filter(matches)
        ScrollView {
            VStack(alignment: .leading, spacing: 10) {
                Toggle("Filtrar por fechas", isOn: $useRange)
                if useRange {
                    DatePicker("Desde", selection: $from, displayedComponents: .date)
                    DatePicker("Hasta", selection: $to, displayedComponents: .date)
                }
                Picker("Tipo", selection: $cat) {
                    Text("Todos los tipos").tag("")
                    ForEach(Catalog.categories.filter { c in apps.contains { $0.category == c.key } }, id: \.key) { Text("\($0.emoji) \($0.name)").tag($0.key) }
                }.pickerStyle(.menu)
                HStack(spacing: 8) {
                    StatCard(label: "Registros", value: "\(list.count)", emoji: "🧾")
                    StatCard(label: "Hectáreas", value: Calculator.fmt(list.reduce(0.0) { $0 + $1.areaHa }), emoji: "🌾")
                    StatCard(label: "Costo", value: money(list.reduce(0.0) { $0 + ($1.cost ?? 0) }), emoji: "💲")
                }
                if list.isEmpty { HelpBox(text: "No hay aplicaciones con estos filtros.") }
                ForEach(list) { a in
                    NavigationLink { ApplicationDetailView(app: a) } label: {
                        ChoiceRow(title: "\(Catalog.category(a.category).emoji) \(a.productName)",
                                  subtitle: "\(dateFmt.string(from: a.date)) · \(a.clientName.isEmpty ? "Sin cliente" : a.clientName)\(a.plotName.isEmpty ? "" : " / " + a.plotName) · \(a.crop.short)",
                                  trailing: "\(Calculator.fmt(a.areaHa)) ha\n\(Calculator.fmtQty(a.productTotal, a.productBaseUnit))") {}
                            .allowsHitTesting(false)
                    }.buttonStyle(.plain)
                }
            }.padding(16)
        }
        .background(Color.agBg)
        .navigationTitle("📋 Historial")
        .searchable(text: $q, prompt: "Cliente, parcela, cultivo, producto…")
    }
}

func reportText(_ a: ApplicationRecord) -> String {
    var s = "🌱 AgroSense · Registro de aplicación\n"
    s += "Fecha: \(dateTimeFmt.string(from: a.date))\n"
    s += "Cliente: \(a.clientName.isEmpty ? "—" : a.clientName) · Parcela: \(a.plotName.isEmpty ? "—" : a.plotName)\n"
    s += "Cultivo: \(a.crop) · Objetivo: \(a.target.isEmpty ? Catalog.category(a.category).name : a.target)\n"
    s += "Producto: \(a.productName) (\(a.active))\n"
    s += "Dosis: \(Calculator.fmt(a.dose, 3)) \(a.doseUnit) · Superficie: \(Calculator.fmt(a.areaHa, 4)) ha\n"
    s += "Producto total: \(Calculator.fmtQty(a.productTotal, a.productBaseUnit)) · Agua: \(a.waterTotalL.map { Calculator.fmt($0, 1) } ?? "—") L · Tanques: \(a.tanks.map(String.init) ?? "—")\n"
    s += "Equipo: \(a.equipment) · Tiempo: \(a.durationMin.map { Calculator.fmtMinutes($0) } ?? "—") · Costo: \(money(a.cost))\n"
    if let r = a.reentryAt { s += "Reentrada: \(dateTimeFmt.string(from: r))\n" }
    if let h = a.harvestAllowedAt { s += "Cosecha permitida desde: \(dateFmt.string(from: h))\n" }
    if !a.operatorName.isEmpty { s += "Aplicador: \(a.operatorName)\n" }
    if !a.weather.isEmpty { s += "Clima: \(a.weather)\n" }
    if !a.ppe.isEmpty { s += "EPP: \(a.ppe.replacingOccurrences(of: "|", with: ", "))\n" }
    if !a.notes.isEmpty { s += "Notas: \(a.notes)\n" }
    return s
}

struct ApplicationDetailView: View {
    let app: ApplicationRecord
    @Environment(\.modelContext) private var ctx
    @Environment(\.dismiss) private var dismiss
    @State private var confirm = false

    var body: some View {
        let now = Date()
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                Text(dateTimeFmt.string(from: app.date)).foregroundStyle(Color.agMuted)
                if let r = app.reentryAt, r > now { HelpBox(text: "🚷 No entrar a la parcela hasta \(dateTimeFmt.string(from: r))", warn: true) }
                if let h = app.harvestAllowedAt, h > now { HelpBox(text: "🧺 No cosechar antes del \(dateFmt.string(from: h))", warn: true) }
                HStack(spacing: 10) {
                    StatCard(label: "Producto", value: Calculator.fmtQty(app.productTotal, app.productBaseUnit), emoji: "🧪")
                    StatCard(label: "Agua", value: "\(app.waterTotalL.map { Calculator.fmt($0, 1) } ?? "—") L", emoji: "💧")
                }
                SectionCard(title: "Detalle", emoji: "📄") { Text(reportText(app)).font(.subheadline) }
                SectionCard(title: "Cuidados posteriores", emoji: "🧼") { ForEach(Safety.afterApplying(app.category), id: \.self) { Text("• \($0)").font(.subheadline) } }
                ShareLink(item: reportText(app)) {
                    Label("Compartir reporte (WhatsApp, correo…)", systemImage: "square.and.arrow.up").fontWeight(.bold).foregroundStyle(.white)
                        .frame(maxWidth: .infinity, minHeight: 54)
                        .background(LinearGradient(colors: Rgb.blue, startPoint: .leading, endPoint: .trailing))
                        .clipShape(RoundedRectangle(cornerRadius: 14))
                }
            }.padding(16)
        }
        .background(Color.agBg)
        .navigationTitle("\(Catalog.category(app.category).emoji) \(app.productName)")
        .toolbar { ToolbarItem { Button(role: .destructive) { confirm = true } label: { Image(systemName: "trash") } } }
        .alert("¿Eliminar registro?", isPresented: $confirm) {
            Button("Eliminar", role: .destructive) { ctx.delete(app); try? ctx.save(); dismiss() }
            Button("Cancelar", role: .cancel) {}
        } message: { Text("Esta acción no se puede deshacer.") }
    }
}

// MARK: - Respaldo (mismo formato JSON que Android)

struct BackupDocument: FileDocument {
    static var readableContentTypes: [UTType] { [.json, .commaSeparatedText] }
    var data: Data
    init(data: Data) { self.data = data }
    init(configuration: ReadConfiguration) throws { data = configuration.file.regularFileContents ?? Data() }
    func fileWrapper(configuration: WriteConfiguration) throws -> FileWrapper { FileWrapper(regularFileWithContents: data) }
}

enum Backup {
    private static func ms(_ d: Date?) -> Any { d.map { Int64($0.timeIntervalSince1970 * 1000) } ?? NSNull() }
    private static func n(_ v: Any?) -> Any { v ?? NSNull() }

    static func export(_ ctx: ModelContext) throws -> Data {
        let c = try ctx.fetch(FetchDescriptor<Client>()).map { ["id": $0.id, "name": $0.name, "phone": $0.phone, "location": $0.location, "notes": $0.notes, "createdAt": ms($0.createdAt)] as [String: Any] }
        let p = try ctx.fetch(FetchDescriptor<Plot>()).map { ["id": $0.id, "clientId": $0.clientId, "name": $0.name, "crop": $0.crop, "variety": $0.variety, "areaHa": $0.areaHa,
                                                             "cycleStart": ms($0.cycleStart), "cycleEnd": ms($0.cycleEnd), "notes": $0.notes] as [String: Any] }
        let pr = try ctx.fetch(FetchDescriptor<Product>()).map { x -> [String: Any] in
            ["id": x.id, "name": x.name, "active": x.active, "category": x.category, "crop": x.crop, "target": x.target, "scientific": x.scientific,
             "doseMin": x.doseMin, "doseMax": x.doseMax, "unit": x.unit, "method": x.method, "formulation": x.formulation, "registration": x.registration,
             "toxBand": x.toxBand, "phiDays": n(x.phiDays), "reiHours": n(x.reiHours), "maxApps": n(x.maxApps), "intervalDays": n(x.intervalDays),
             "price": n(x.price), "priceUnit": x.priceUnit, "source": x.source]
        }
        let a = try ctx.fetch(FetchDescriptor<ApplicationRecord>()).map { x -> [String: Any] in
            ["id": x.id, "clientId": n(x.clientId), "plotId": n(x.plotId), "clientName": x.clientName, "plotName": x.plotName, "crop": x.crop,
             "category": x.category, "productId": n(x.productId), "productName": x.productName, "active": x.active, "target": x.target,
             "date": ms(x.date), "areaHa": x.areaHa, "dose": x.dose, "doseUnit": x.doseUnit, "productTotal": x.productTotal,
             "productBaseUnit": x.productBaseUnit, "waterTotalL": n(x.waterTotalL), "waterLHa": n(x.waterLHa), "tanks": n(x.tanks),
             "tankL": n(x.tankL), "equipment": x.equipment, "durationMin": n(x.durationMin), "cost": n(x.cost), "operator": x.operatorName,
             "weather": x.weather, "ppe": x.ppe, "phiDays": n(x.phiDays), "reiHours": n(x.reiHours), "harvestAllowedAt": ms(x.harvestAllowedAt),
             "reentryAt": ms(x.reentryAt), "notes": x.notes]
        }
        let root: [String: Any] = ["app": "AgroSense", "version": 1, "exportedAt": ms(.now), "clients": c, "plots": p, "products": pr, "applications": a]
        return try JSONSerialization.data(withJSONObject: root, options: [.prettyPrinted, .sortedKeys])
    }

    static func importData(_ data: Data, _ ctx: ModelContext) throws {
        guard let root = try JSONSerialization.jsonObject(with: data) as? [String: Any], root["app"] as? String == "AgroSense" else {
            throw NSError(domain: "AgroSense", code: 1, userInfo: [NSLocalizedDescriptionKey: "El archivo no es un respaldo de AgroSense."])
        }
        func arr(_ k: String) -> [[String: Any]] { root[k] as? [[String: Any]] ?? [] }
        func s(_ o: [String: Any], _ k: String) -> String { o[k] as? String ?? "" }
        func d(_ o: [String: Any], _ k: String) -> Double? { (o[k] as? NSNumber)?.doubleValue }
        func i(_ o: [String: Any], _ k: String) -> Int? { (o[k] as? NSNumber)?.intValue }
        func l(_ o: [String: Any], _ k: String) -> Int64? { (o[k] as? NSNumber)?.int64Value }
        func dt(_ o: [String: Any], _ k: String) -> Date? { l(o, k).map { Date(timeIntervalSince1970: Double($0) / 1000) } }

        try ctx.delete(model: ApplicationRecord.self); try ctx.delete(model: Plot.self)
        try ctx.delete(model: Client.self); try ctx.delete(model: Product.self)
        for o in arr("clients") { ctx.insert(Client(id: l(o, "id") ?? 0, name: s(o, "name"), phone: s(o, "phone"), location: s(o, "location"), notes: s(o, "notes"), createdAt: dt(o, "createdAt") ?? .now)) }
        for o in arr("plots") { ctx.insert(Plot(id: l(o, "id") ?? 0, clientId: l(o, "clientId") ?? 0, name: s(o, "name"), crop: s(o, "crop"), variety: s(o, "variety"), areaHa: d(o, "areaHa") ?? 0, cycleStart: dt(o, "cycleStart"), cycleEnd: dt(o, "cycleEnd"), notes: s(o, "notes"))) }
        for o in arr("products") {
            ctx.insert(Product(id: l(o, "id") ?? 0, name: s(o, "name"), active: s(o, "active"), category: s(o, "category"), crop: s(o, "crop"), target: s(o, "target"),
                               scientific: s(o, "scientific"), doseMin: d(o, "doseMin") ?? 0, doseMax: d(o, "doseMax") ?? 0, unit: s(o, "unit"), method: s(o, "method"),
                               formulation: s(o, "formulation"), registration: s(o, "registration"), toxBand: s(o, "toxBand"), phiDays: i(o, "phiDays"),
                               reiHours: d(o, "reiHours"), maxApps: i(o, "maxApps"), intervalDays: i(o, "intervalDays"), price: d(o, "price"),
                               priceUnit: s(o, "priceUnit").isEmpty ? "L" : s(o, "priceUnit"), source: s(o, "source").isEmpty ? "Usuario" : s(o, "source")))
        }
        for o in arr("applications") {
            ctx.insert(ApplicationRecord(id: l(o, "id") ?? 0, clientId: l(o, "clientId"), plotId: l(o, "plotId"), clientName: s(o, "clientName"), plotName: s(o, "plotName"),
                                         crop: s(o, "crop"), category: s(o, "category"), productId: l(o, "productId"), productName: s(o, "productName"), active: s(o, "active"),
                                         target: s(o, "target"), date: dt(o, "date") ?? .now, areaHa: d(o, "areaHa") ?? 0, dose: d(o, "dose") ?? 0, doseUnit: s(o, "doseUnit"),
                                         productTotal: d(o, "productTotal") ?? 0, productBaseUnit: s(o, "productBaseUnit"), waterTotalL: d(o, "waterTotalL"), waterLHa: d(o, "waterLHa"),
                                         tanks: i(o, "tanks"), tankL: d(o, "tankL"), equipment: s(o, "equipment"), durationMin: d(o, "durationMin"), cost: d(o, "cost"),
                                         operatorName: s(o, "operator"), weather: s(o, "weather"), ppe: s(o, "ppe"), phiDays: i(o, "phiDays"), reiHours: d(o, "reiHours"),
                                         harvestAllowedAt: dt(o, "harvestAllowedAt"), reentryAt: dt(o, "reentryAt"), notes: s(o, "notes")))
        }
        try ctx.save()
    }

    static func csv(_ ctx: ModelContext) throws -> Data {
        let f = DateFormatter(); f.dateFormat = "yyyy-MM-dd"
        let header = ["Fecha", "Cliente", "Parcela", "Cultivo", "Categoría", "Plaga/objetivo", "Producto", "Ingrediente activo", "Superficie_ha", "Dosis", "Unidad_dosis",
                      "Producto_total", "Unidad", "Agua_L", "Tanques", "Equipo", "Duración_min", "Costo_MXN", "Responsable", "Clima", "EPP", "Cosecha_permitida", "Observaciones"]
        let rows = try ctx.fetch(FetchDescriptor<ApplicationRecord>(sortBy: [SortDescriptor(\.date, order: .reverse)])).map { a -> [String] in
            [f.string(from: a.date), a.clientName, a.plotName, a.crop, a.category, a.target, a.productName, a.active, "\(a.areaHa)", "\(a.dose)", a.doseUnit,
             "\(a.productTotal)", a.productBaseUnit, a.waterTotalL.map { "\($0)" } ?? "", a.tanks.map { "\($0)" } ?? "", a.equipment,
             a.durationMin.map { "\($0)" } ?? "", a.cost.map { "\($0)" } ?? "", a.operatorName, a.weather, a.ppe.replacingOccurrences(of: "|", with: "; "),
             a.harvestAllowedAt.map { f.string(from: $0) } ?? "", a.notes]
        }
        let text = "\u{FEFF}" + ([header] + rows).map { $0.map { "\"" + $0.replacingOccurrences(of: "\"", with: "\"\"") + "\"" }.joined(separator: ",") }.joined(separator: "\r\n")
        return Data(text.utf8)
    }
}

struct BackupView: View {
    @Environment(\.modelContext) private var ctx
    @State private var exportDoc: BackupDocument? = nil
    @State private var exportName = "AgroSense_respaldo"
    @State private var exportType: UTType = .json
    @State private var showExporter = false
    @State private var showImporter = false
    @State private var pendingData: Data? = nil
    @State private var message: String? = nil

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                HelpBox(text: "Todos tus datos se guardan en la memoria interna del iPhone, aunque no tengas internet. Haz un respaldo seguido para no perderlos si cambias de equipo.")
                RgbButton(title: "💾 Guardar respaldo (JSON)", colors: Rgb.green) {
                    do { exportDoc = BackupDocument(data: try Backup.export(ctx)); exportName = "AgroSense_respaldo"; exportType = .json; showExporter = true }
                    catch { message = error.localizedDescription }
                }
                RgbButton(title: "📊 Exportar historial a Excel (CSV)", colors: Rgb.blue) {
                    do { exportDoc = BackupDocument(data: try Backup.csv(ctx)); exportName = "AgroSense_historial"; exportType = .commaSeparatedText; showExporter = true }
                    catch { message = error.localizedDescription }
                }
                RgbButton(title: "📥 Restaurar respaldo", colors: Rgb.red) { showImporter = true }
                Text("El respaldo es compatible con la versión de Android.").font(.footnote).foregroundStyle(Color.agMuted)
            }.padding(16)
        }
        .background(Color.agBg)
        .navigationTitle("⚙️ Respaldo")
        .fileExporter(isPresented: $showExporter, document: exportDoc, contentType: exportType, defaultFilename: exportName) { r in
            if case .failure(let e) = r { message = e.localizedDescription } else { message = "Archivo guardado ✅" }
        }
        .fileImporter(isPresented: $showImporter, allowedContentTypes: [.json]) { r in
            switch r {
            case .success(let url):
                let ok = url.startAccessingSecurityScopedResource()
                defer { if ok { url.stopAccessingSecurityScopedResource() } }
                pendingData = try? Data(contentsOf: url)
                if pendingData == nil { message = "No se pudo leer el archivo." }
            case .failure(let e): message = e.localizedDescription
            }
        }
        .alert("¿Restaurar respaldo?", isPresented: Binding(get: { pendingData != nil }, set: { if !$0 { pendingData = nil } })) {
            Button("Restaurar", role: .destructive) {
                if let d = pendingData {
                    do { try Backup.importData(d, ctx); message = "Respaldo restaurado ✅" } catch { message = "Archivo no válido: \(error.localizedDescription)" }
                }
                pendingData = nil
            }
            Button("Cancelar", role: .cancel) { pendingData = nil }
        } message: { Text("Se reemplazarán TODOS los clientes, parcelas, productos y aplicaciones actuales por los del archivo.") }
        .alert(message ?? "", isPresented: Binding(get: { message != nil }, set: { if !$0 { message = nil } })) { Button("OK") { message = nil } }
    }
}
