import Foundation

/// Motor de cálculo (equivalente a Calculator.kt). Producto en unidad base: mL (líquidos) o g (sólidos).
enum Calculator {
    struct Input {
        var dose: Double; var unit: String; var areaHa: Double; var waterLHa: Double
        var tankL: Double; var flowLMin: Double; var refillMin: Double
        var price: Double? = nil; var priceUnit: String = "L"
    }

    struct Result {
        let productBase: Double, baseUnit: String, waterTotalL: Double, productPerLiter: Double
        let fullTanks: Int, lastTankL: Double, tanksTotal: Int, productPerFullTank: Double, productLastTank: Double
        let sprayMin: Double, refillTotalMin: Double, totalMin: Double, cost: Double?
    }

    enum Outcome { case ok(Result), error(String) }

    static func isPerArea(_ u: String) -> Bool { u.hasSuffix("/ha") }
    static func isLiquid(_ u: String) -> Bool { u.hasPrefix("L") || u.hasPrefix("mL") || u == "% v/v" }
    static func baseUnit(_ u: String) -> String { isLiquid(u) ? "mL" : "g" }
    private static func toBase(_ u: String) -> Double { (u.hasPrefix("L/") || u.hasPrefix("kg/")) ? 1000 : 1 }
    private static func perLiter(_ d: Double, _ u: String) -> Double {
        switch u {
        case "mL/100L", "g/100L": return d / 100
        case "% v/v", "% p/v": return d * 10   // 1 % = 10 mL (o g) por litro de caldo
        default: return d                      // mL/L, g/L
        }
    }

    static func calculate(_ i: Input) -> Outcome {
        guard i.dose > 0 else { return .error("La dosis debe ser mayor que cero.") }
        guard i.areaHa > 0 else { return .error("Escribe la superficie a tratar.") }
        guard i.waterLHa > 0 else { return .error("Escribe el volumen de agua por hectárea (calibración).") }
        guard i.tankL > 0 else { return .error("Escribe la capacidad del tanque.") }
        let water = i.waterLHa * i.areaHa
        let base = baseUnit(i.unit)
        let total: Double, perL: Double
        if isPerArea(i.unit) { total = i.dose * toBase(i.unit) * i.areaHa; perL = total / water }
        else { perL = perLiter(i.dose, i.unit); total = perL * water }
        let full = Int(floor(water / i.tankL + 1e-9))
        var last = water - Double(full) * i.tankL
        if last < 1e-6 { last = 0 }
        let tanks = full + (last > 0 ? 1 : 0)
        let spray = i.flowLMin > 0 ? water / i.flowLMin : 0
        let refill = Double(tanks) * i.refillMin
        return .ok(Result(productBase: total, baseUnit: base, waterTotalL: water, productPerLiter: perL, fullTanks: full,
                          lastTankL: last, tanksTotal: tanks, productPerFullTank: perL * i.tankL, productLastTank: perL * last,
                          sprayMin: spray, refillTotalMin: refill, totalMin: spray + refill,
                          cost: cost(total, base, i.price, i.priceUnit)))
    }

    static func cost(_ totalBase: Double, _ base: String, _ price: Double?, _ priceUnit: String) -> Double? {
        guard let price, price > 0 else { return nil }
        let liquidPrice = priceUnit == "L" || priceUnit == "mL"
        if (base == "mL") != liquidPrice { return nil }
        let factor: Double = (priceUnit == "L" || priceUnit == "kg") ? 1000 : 1
        return totalBase / factor * price
    }

    static func fmt(_ v: Double, _ dec: Int = 2) -> String {
        let f = NumberFormatter()
        f.locale = Locale(identifier: "en_US"); f.numberStyle = .decimal
        f.maximumFractionDigits = dec; f.minimumFractionDigits = 0
        return f.string(from: NSNumber(value: v)) ?? "\(v)"
    }
    /// Para precargar campos de texto (sin separador de miles).
    static func plain(_ v: Double, _ dec: Int = 4) -> String { fmt(v, dec).replacingOccurrences(of: ",", with: "") }

    static func fmtQty(_ base: Double, _ unit: String) -> String {
        let big = unit == "mL" ? "L" : "kg"
        return base >= 1000 ? "\(fmt(base / 1000, 2)) \(big)" : "\(fmt(base, base < 10 ? 2 : 1)) \(unit)"
    }

    static func fmtMinutes(_ m: Double) -> String {
        let t = Int(ceil(m))
        return t < 60 ? "\(t) min" : "\(t / 60) h \(t % 60) min"
    }
}

/// Sugerencias de seguridad (equivalente a Safety.kt). La etiqueta siempre manda.
enum Safety {
    static func ppe(category: String, formulation: String, toxBand: String, equipment: String) -> [String] {
        var l = ["Overol de manga larga o traje desechable", "Guantes de nitrilo (nunca de tela o piel)",
                 "Botas de hule con el pantalón por fuera", "Lentes de seguridad o goggles", "Gorra o capucha impermeable"]
        let f = formulation.uppercased()
        let powder = ["WP", "WG", "DP", "SP", "GR", "PH"].contains { f.contains($0) }
        l.append(powder ? "Respirador con filtro para partículas (N95/P100) al pesar el polvo" : "Careta facial y mandil impermeable al preparar la mezcla")
        if ["Rojo", "Amarillo"].contains(toxBand) || ["turbo", "motobomba"].contains(equipment) {
            l.append("Respirador de media cara con cartuchos para vapores orgánicos + prefiltro")
        }
        if toxBand == "Rojo" { l.append("Traje impermeable completo; nunca aplicar solo, ten a alguien cerca") }
        switch category {
        case "rodenticida", "molusquicida": l.append("Guantes al colocar cebos y cebaderas rotuladas")
        case "nematicida": l.append("Evitar contacto con suelo tratado: botas y guantes hasta terminar")
        case "piscicida", "alguicida": l.append("Botas o vadeadores impermeables si entras al agua")
        default: break
        }
        if equipment == "dron" { l.append("Operador a distancia segura y área despejada de personas") }
        return l
    }

    static func beforeApplying(_ equipment: String) -> [String] {[
        "Lee la etiqueta completa y confirma que el cultivo y la plaga estén autorizados.",
        "Calibra el equipo con agua limpia y revisa fugas en boquillas y mangueras.",
        "Prepara la mezcla al aire libre, lejos de pozos, ríos y casas.",
        "Llena el tanque a la mitad con agua, agrega el producto y completa con agua mientras agitas.",
        equipment == "dron" ? "Verifica permisos y que la etiqueta autorice aplicación aérea o con dron."
                            : "Ten a la mano agua limpia y jabón para lavarte en caso de contacto."
    ]}

    static let bestTime = [
        "🕕 Mejor horario: temprano (6–10 h) o al atardecer (17–19 h).",
        "🌬️ Viento ideal entre 3 y 10 km/h. No apliques con más de 15 km/h.",
        "🌡️ Temperatura menor a 30 °C y humedad relativa mayor a 50 %.",
        "🌧️ Sin lluvia pronosticada en las próximas 4–6 horas (o lo que indique la etiqueta).",
        "🐝 Evita horas con abejas activas y la floración abierta."
    ]

    static func afterApplying(_ category: String) -> [String] {
        Catalog.category(category).care + [
            "Haz el triple lavado del envase, perfóralo y entrégalo a un centro de acopio.",
            "Lava el equipo lejos de fuentes de agua y vierte el enjuague en la parcela tratada.",
            "Báñate con agua y jabón; lava la ropa de trabajo aparte de la ropa de la familia.",
            "Señaliza la parcela y respeta el tiempo de reentrada y el intervalo de seguridad.",
            "No comas, bebas ni fumes hasta lavarte las manos y la cara.",
            "Si alguien presenta mareo, náusea o irritación, retíralo del área y llama al número de emergencia de la etiqueta (en México, SINTOX)."
        ]
    }

    static func warnings(product p: Product?, plotId: Int64?, history: [ApplicationRecord], date: Date, dose: Double) -> [String] {
        guard let p else { return [] }
        var w: [String] = []
        let cat = Catalog.category(p.category)
        if cat.restricted { w.append("⛔ \(cat.name): uso restringido. Requiere autorización vigente y seguir estrictamente la etiqueta.") }
        if dose > p.doseMax + 1e-9 { w.append("⛔ La dosis supera el máximo de etiqueta (\(Calculator.fmt(p.doseMax)) \(p.unit)).") }
        if dose < p.doseMin - 1e-9 { w.append("⚠️ La dosis está por debajo del mínimo de etiqueta: puede no controlar y generar resistencia.") }
        guard let plotId else { return w }
        let onPlot = history.filter { $0.plotId == plotId }
        let same = onPlot.filter { $0.productName.caseInsensitiveCompare(p.name) == .orderedSame }
        if let max = p.maxApps, same.count >= max {
            w.append("⛔ Ya se registraron \(same.count) aplicaciones de \(p.name) en esta parcela (máximo de etiqueta: \(max)).")
        }
        if let iv = p.intervalDays, let last = same.max(by: { $0.date < $1.date }) {
            let days = Int(date.timeIntervalSince(last.date) / 86_400)
            if days < iv { w.append("⚠️ Han pasado \(days) días desde la última aplicación de este producto; la etiqueta pide al menos \(iv).") }
        }
        let lastTwo = Array(onPlot.sorted { $0.date > $1.date }.prefix(2))
        if !p.active.isEmpty, lastTwo.count == 2, lastTwo.allSatisfy({ $0.active.caseInsensitiveCompare(p.active) == .orderedSame }) {
            w.append("🔁 Sería la 3.ª aplicación seguida con \(p.active). Rota el modo de acción para evitar resistencia.")
        }
        return w
    }
}
