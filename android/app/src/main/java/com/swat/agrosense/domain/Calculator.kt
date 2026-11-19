package com.swat.agrosense.domain

import kotlin.math.ceil
import kotlin.math.floor

/**
 * Motor de cálculo de AgroSense. Lógica pura (sin Android) para poder probarla.
 * Todas las cantidades de producto se manejan internamente en unidad base: mL (líquidos) o g (sólidos).
 */
object Calculator {

    data class Input(
        val dose: Double,          // dosis elegida dentro del rango de etiqueta
        val unit: String,          // L/ha, mL/ha, kg/ha, g/ha, mL/100L, g/100L, mL/L, g/L, % v/v, % p/v
        val areaHa: Double,
        val waterLHa: Double,      // volumen de agua calibrado
        val tankL: Double,
        val flowLMin: Double,      // gasto del equipo
        val refillMin: Double,     // tiempo de preparación/recarga por tanque
        val price: Double? = null, // precio por priceUnit
        val priceUnit: String = "L"
    )

    data class Result(
        val productBase: Double,       // total en mL o g
        val baseUnit: String,          // "mL" o "g"
        val waterTotalL: Double,
        val productPerLiter: Double,   // mL o g por litro de caldo
        val fullTanks: Int,
        val lastTankL: Double,         // agua del último tanque parcial (0 si no hay)
        val tanksTotal: Int,
        val productPerFullTank: Double,
        val productLastTank: Double,
        val sprayMin: Double,
        val refillTotalMin: Double,
        val totalMin: Double,
        val cost: Double?
    )

    sealed class Outcome {
        data class Ok(val r: Result) : Outcome()
        data class Error(val message: String) : Outcome()
    }

    fun isPerArea(unit: String) = unit.endsWith("/ha")
    fun isLiquid(unit: String) = unit.startsWith("L") || unit.startsWith("mL") || unit == "% v/v"
    fun baseUnit(unit: String) = if (isLiquid(unit)) "mL" else "g"

    /** Factor para pasar la unidad de producto de la dosis a unidad base (mL o g). */
    private fun toBase(unit: String): Double = when {
        unit.startsWith("L/") || unit.startsWith("kg/") -> 1000.0
        else -> 1.0
    }

    /** Producto (unidad base) por litro de agua para dosis expresadas sobre volumen. */
    private fun perLiter(dose: Double, unit: String): Double = when (unit) {
        "mL/100L", "g/100L" -> dose / 100.0
        "mL/L", "g/L" -> dose
        "% v/v", "% p/v" -> dose * 10.0 // 1 % = 10 mL (o g) por litro de caldo
        else -> error("Unidad no volumétrica: $unit")
    }

    fun calculate(i: Input): Outcome {
        if (!(i.dose > 0)) return Outcome.Error("La dosis debe ser mayor que cero.")
        if (!(i.areaHa > 0)) return Outcome.Error("Escribe la superficie a tratar.")
        if (!(i.waterLHa > 0)) return Outcome.Error("Escribe el volumen de agua por hectárea (calibración).")
        if (!(i.tankL > 0)) return Outcome.Error("Escribe la capacidad del tanque.")
        val water = i.waterLHa * i.areaHa
        val base = baseUnit(i.unit)
        val total: Double
        val perL: Double
        if (isPerArea(i.unit)) {
            total = i.dose * toBase(i.unit) * i.areaHa
            perL = total / water
        } else {
            perL = perLiter(i.dose, i.unit)
            total = perL * water
        }
        val full = floor(water / i.tankL + 1e-9).toInt()
        var last = water - full * i.tankL
        if (last < 1e-6) last = 0.0
        val tanks = full + if (last > 0) 1 else 0
        val spray = if (i.flowLMin > 0) water / i.flowLMin else 0.0
        val refill = tanks * i.refillMin
        return Outcome.Ok(
            Result(
                productBase = total, baseUnit = base, waterTotalL = water, productPerLiter = perL,
                fullTanks = full, lastTankL = last, tanksTotal = tanks,
                productPerFullTank = perL * i.tankL, productLastTank = perL * last,
                sprayMin = spray, refillTotalMin = refill, totalMin = spray + refill,
                cost = cost(total, base, i.price, i.priceUnit)
            )
        )
    }

    /** Costo: convierte el total (mL o g) a la unidad de precio. Null si no hay precio o las unidades no son compatibles. */
    fun cost(totalBase: Double, base: String, price: Double?, priceUnit: String): Double? {
        if (price == null || price <= 0) return null
        val liquidPrice = priceUnit == "L" || priceUnit == "mL"
        if ((base == "mL") != liquidPrice) return null
        val factor = when (priceUnit) { "L", "kg" -> 1000.0; else -> 1.0 }
        return totalBase / factor * price
    }

    /** Muestra mL/g en L/kg cuando conviene. */
    fun fmtQty(base: Double, unit: String): String {
        val big = if (unit == "mL") "L" else "kg"
        return if (base >= 1000) "${fmt(base / 1000, 2)} $big" else "${fmt(base, if (base < 10) 2 else 1)} $unit"
    }

    fun fmt(v: Double, dec: Int = 2): String {
        val s = String.format(java.util.Locale.US, "%,.${dec}f", v)
        return if (s.contains('.')) s.trimEnd('0').trimEnd('.') else s
    }

    fun fmtMinutes(min: Double): String {
        val m = ceil(min).toInt()
        return if (m < 60) "$m min" else "${m / 60} h ${m % 60} min"
    }
}
