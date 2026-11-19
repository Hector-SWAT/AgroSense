package com.swat.agrosense

import com.swat.agrosense.domain.Calculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorTest {
    private fun ok(i: Calculator.Input) = (Calculator.calculate(i) as Calculator.Outcome.Ok).r

    @Test fun dosisPorHectarea_litros() {
        // Select Ultra 1.5 L/ha en 2,500 m² con 400 L/ha y mochila de 20 L
        val r = ok(Calculator.Input(1.5, "L/ha", 0.25, 400.0, 20.0, 0.8, 6.0, price = 500.0, priceUnit = "L"))
        assertEquals(375.0, r.productBase, 1e-9)          // mL
        assertEquals("mL", r.baseUnit)
        assertEquals(100.0, r.waterTotalL, 1e-9)
        assertEquals(5, r.tanksTotal)
        assertEquals(75.0, r.productPerFullTank, 1e-9)    // mL por tanque de 20 L
        assertEquals(187.5, r.cost!!, 1e-9)
        assertEquals(125.0 + 30.0, r.totalMin, 1e-9)
    }

    @Test fun dosisPorHectarea_gramos_conTanqueParcial() {
        // Amistar 300 g/ha, 1.2 ha, 450 L/ha, tractor 400 L
        val r = ok(Calculator.Input(300.0, "g/ha", 1.2, 450.0, 400.0, 25.0, 20.0))
        assertEquals(360.0, r.productBase, 1e-9)
        assertEquals(540.0, r.waterTotalL, 1e-9)
        assertEquals(1, r.fullTanks)
        assertEquals(140.0, r.lastTankL, 1e-9)
        assertEquals(2, r.tanksTotal)
        assertEquals(r.productBase, r.productPerFullTank + r.productLastTank, 1e-9)
    }

    @Test fun dosisPorVolumen_mLporLitro() {
        // Candor 3 mL/L, 0.5 ha, 500 L/ha
        val r = ok(Calculator.Input(3.0, "mL/L", 0.5, 500.0, 20.0, 0.8, 5.0))
        assertEquals(750.0, r.productBase, 1e-9)
    }

    @Test fun porcentajeYCien() {
        assertEquals(2500.0, ok(Calculator.Input(1.0, "% v/v", 0.5, 500.0, 20.0, 1.0, 0.0)).productBase, 1e-9)
        assertEquals(500.0, ok(Calculator.Input(200.0, "g/100L", 0.5, 500.0, 20.0, 1.0, 0.0)).productBase, 1e-9)
    }

    @Test fun costoConUnidadIncompatible() {
        assertEquals(null, Calculator.cost(500.0, "g", 100.0, "L"))
        assertEquals(50.0, Calculator.cost(500.0, "g", 100.0, "kg")!!, 1e-9)
    }

    @Test fun errores() {
        assertTrue(Calculator.calculate(Calculator.Input(1.0, "L/ha", 0.0, 400.0, 20.0, 1.0, 1.0)) is Calculator.Outcome.Error)
        assertTrue(Calculator.calculate(Calculator.Input(1.0, "L/ha", 1.0, 400.0, 0.0, 1.0, 1.0)) is Calculator.Outcome.Error)
    }
}
