import Foundation
import SwiftData

/// Cliente (productor). Los `id` numéricos mantienen compatibilidad con el respaldo JSON de Android.
@Model final class Client {
    @Attribute(.unique) var id: Int64
    var name: String
    var phone: String
    var location: String
    var notes: String
    var createdAt: Date

    init(id: Int64, name: String, phone: String = "", location: String = "", notes: String = "", createdAt: Date = .now) {
        self.id = id; self.name = name; self.phone = phone; self.location = location; self.notes = notes; self.createdAt = createdAt
    }
}

@Model final class Plot {
    @Attribute(.unique) var id: Int64
    var clientId: Int64
    var name: String
    var crop: String
    var variety: String
    var areaHa: Double
    var cycleStart: Date?
    var cycleEnd: Date?
    var notes: String

    init(id: Int64, clientId: Int64, name: String, crop: String, variety: String = "", areaHa: Double, cycleStart: Date? = nil, cycleEnd: Date? = nil, notes: String = "") {
        self.id = id; self.clientId = clientId; self.name = name; self.crop = crop; self.variety = variety
        self.areaHa = areaHa; self.cycleStart = cycleStart; self.cycleEnd = cycleEnd; self.notes = notes
    }
}

/// Uso autorizado según etiqueta: producto + cultivo + plaga + dosis.
@Model final class Product {
    @Attribute(.unique) var id: Int64
    var name: String
    var active: String
    var category: String
    var crop: String
    var target: String
    var scientific: String
    var doseMin: Double
    var doseMax: Double
    var unit: String
    var method: String
    var formulation: String
    var registration: String
    var toxBand: String
    var phiDays: Int?
    var reiHours: Double?
    var maxApps: Int?
    var intervalDays: Int?
    var price: Double?
    var priceUnit: String
    var source: String

    init(id: Int64, name: String, active: String = "", category: String, crop: String = "", target: String = "", scientific: String = "",
         doseMin: Double, doseMax: Double, unit: String, method: String = "", formulation: String = "", registration: String = "",
         toxBand: String = "", phiDays: Int? = nil, reiHours: Double? = nil, maxApps: Int? = nil, intervalDays: Int? = nil,
         price: Double? = nil, priceUnit: String = "L", source: String = "Usuario") {
        self.id = id; self.name = name; self.active = active; self.category = category; self.crop = crop; self.target = target
        self.scientific = scientific; self.doseMin = doseMin; self.doseMax = doseMax; self.unit = unit; self.method = method
        self.formulation = formulation; self.registration = registration; self.toxBand = toxBand; self.phiDays = phiDays
        self.reiHours = reiHours; self.maxApps = maxApps; self.intervalDays = intervalDays; self.price = price
        self.priceUnit = priceUnit; self.source = source
    }
}

@Model final class ApplicationRecord {
    @Attribute(.unique) var id: Int64
    var clientId: Int64?
    var plotId: Int64?
    var clientName: String
    var plotName: String
    var crop: String
    var category: String
    var productId: Int64?
    var productName: String
    var active: String
    var target: String
    var date: Date
    var areaHa: Double
    var dose: Double
    var doseUnit: String
    var productTotal: Double
    var productBaseUnit: String
    var waterTotalL: Double?
    var waterLHa: Double?
    var tanks: Int?
    var tankL: Double?
    var equipment: String
    var durationMin: Double?
    var cost: Double?
    var operatorName: String
    var weather: String
    var ppe: String
    var phiDays: Int?
    var reiHours: Double?
    var harvestAllowedAt: Date?
    var reentryAt: Date?
    var notes: String

    init(id: Int64, clientId: Int64?, plotId: Int64?, clientName: String, plotName: String, crop: String, category: String,
         productId: Int64?, productName: String, active: String, target: String, date: Date, areaHa: Double, dose: Double,
         doseUnit: String, productTotal: Double, productBaseUnit: String, waterTotalL: Double?, waterLHa: Double?, tanks: Int?,
         tankL: Double?, equipment: String, durationMin: Double?, cost: Double?, operatorName: String, weather: String, ppe: String,
         phiDays: Int?, reiHours: Double?, harvestAllowedAt: Date?, reentryAt: Date?, notes: String) {
        self.id = id; self.clientId = clientId; self.plotId = plotId; self.clientName = clientName; self.plotName = plotName
        self.crop = crop; self.category = category; self.productId = productId; self.productName = productName; self.active = active
        self.target = target; self.date = date; self.areaHa = areaHa; self.dose = dose; self.doseUnit = doseUnit
        self.productTotal = productTotal; self.productBaseUnit = productBaseUnit; self.waterTotalL = waterTotalL; self.waterLHa = waterLHa
        self.tanks = tanks; self.tankL = tankL; self.equipment = equipment; self.durationMin = durationMin; self.cost = cost
        self.operatorName = operatorName; self.weather = weather; self.ppe = ppe; self.phiDays = phiDays; self.reiHours = reiHours
        self.harvestAllowedAt = harvestAllowedAt; self.reentryAt = reentryAt; self.notes = notes
    }
}

/// Genera ids numéricos crecientes (estilo autoincrement de Room).
enum IdGen {
    static func next<T: PersistentModel>(_ type: T.Type, _ ctx: ModelContext, _ key: (T) -> Int64) -> Int64 {
        let all = (try? ctx.fetch(FetchDescriptor<T>())) ?? []
        return (all.map(key).max() ?? 0) + 1
    }
}

enum Seeder {
    static func seedIfNeeded(_ ctx: ModelContext) {
        let count = (try? ctx.fetchCount(FetchDescriptor<Product>())) ?? 0
        guard count == 0 else { return }
        insertCatalog(ctx, skipExisting: false)
    }

    /// Inserta el catálogo de Datos_Completos.xlsx (dosis tal como aparecen en la ficha).
    static func insertCatalog(_ ctx: ModelContext, skipExisting: Bool) {
        let existing = Set(((try? ctx.fetch(FetchDescriptor<Product>())) ?? []).filter { $0.source == "Catálogo" }.map { $0.name + $0.crop + $0.target })
        var nextId = IdGen.next(Product.self, ctx) { $0.id }
        for s in Catalog.labelSeeds where !(skipExisting && existing.contains(s.name + s.crop + s.target)) {
            ctx.insert(Product(id: nextId, name: s.name, active: s.active, category: s.category, crop: s.crop, target: s.target,
                               scientific: s.scientific, doseMin: s.doseMin, doseMax: s.doseMax, unit: s.unit, method: s.note,
                               priceUnit: (s.unit.hasPrefix("g") || s.unit.hasPrefix("kg")) ? "kg" : "L", source: "Catálogo"))
            nextId += 1
        }
        try? ctx.save()
    }
}
