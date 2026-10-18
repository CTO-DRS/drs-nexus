package com.drs.nexus.core.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorToolsTest {

    // ---------- Percentage ----------

    @Test
    fun `percent of value`() {
        val r = PercentageCalculator.calculate(0, 25.0, 200.0) as PercentageCalculator.Result.Success
        assertTrue(r.text.contains("= 50"))
        assertEquals(50.0, r.value, 0.0001)
    }

    @Test
    fun `is what percent`() {
        val r = PercentageCalculator.calculate(1, 50.0, 200.0) as PercentageCalculator.Result.Success
        assertTrue(r.text.contains("25%"))
    }

    @Test
    fun `percent change up and down`() {
        val up = PercentageCalculator.calculate(2, 80.0, 100.0) as PercentageCalculator.Result.Success
        assertTrue(up.text.contains("+25%"))
        val down = PercentageCalculator.calculate(2, 100.0, 75.0) as PercentageCalculator.Result.Success
        assertTrue(down.text.contains("-25%"))
    }

    @Test
    fun `division by zero guarded`() {
        assertTrue(PercentageCalculator.calculate(1, 5.0, 0.0) is PercentageCalculator.Result.Failure)
        assertTrue(PercentageCalculator.calculate(2, 0.0, 5.0) is PercentageCalculator.Result.Failure)
    }

    // ---------- Loan ----------

    @Test
    fun `emi matches known formula`() {
        // 300,000 / 6% / 30y -> classic example: monthly = 1,798.65
        val r = LoanCalculator.calculate(300_000.0, 6.0, 30.0)
        assertTrue(r is LoanCalculator.Result.Success)
        val plan = (r as LoanCalculator.Result.Success).plan
        assertEquals(1798.65, plan.monthlyPayment, 0.02)
        assertEquals(360, plan.months)
        assertEquals(347514.57, plan.totalInterest, 1.0)
    }

    @Test
    fun `zero interest is simple division`() {
        val plan = (LoanCalculator.calculate(12_000.0, 0.0, 2.0) as LoanCalculator.Result.Success).plan
        assertEquals(500.0, plan.monthlyPayment, 0.001)
        assertEquals(0.0, plan.totalInterest, 0.001)
    }

    @Test
    fun `loan validation`() {
        assertTrue(LoanCalculator.calculate(0.0, 5.0, 1.0) is LoanCalculator.Result.Failure)
        assertTrue(LoanCalculator.calculate(1000.0, -1.0, 1.0) is LoanCalculator.Result.Failure)
        assertTrue(LoanCalculator.calculate(1000.0, 5.0, 0.0) is LoanCalculator.Result.Failure)
    }

    // ---------- BMI ----------

    @Test
    fun `bmi normal weight`() {
        // 70 kg, 175 cm -> 22.86
        val r = BmiCalculator.calculate(70.0, 175.0)
        assertTrue(r is BmiCalculator.Result.Success)
        val reading = (r as BmiCalculator.Result.Success).reading
        assertEquals(22.86, reading.bmi, 0.01)
        assertEquals(BmiCalculator.Category.NORMAL, reading.category)
    }

    @Test
    fun `bmi categories boundaries`() {
        fun cat(kg: Double, cm: Double) = (BmiCalculator.calculate(kg, cm) as BmiCalculator.Result.Success).reading.category
        assertEquals(BmiCalculator.Category.UNDERWEIGHT, cat(50.0, 175.0))
        assertEquals(BmiCalculator.Category.OVERWEIGHT, cat(80.0, 175.0))
        assertEquals(BmiCalculator.Category.OBESE, cat(100.0, 175.0))
    }

    @Test
    fun `imperial conversion matches metric`() {
        // 154.32 lb ≈ 70 kg ; 68.9 in ≈ 175 cm
        val imp = (BmiCalculator.imperial(154.32, 68.90) as BmiCalculator.Result.Success).reading
        assertEquals(22.86, imp.bmi, 0.05)
    }

    @Test
    fun `bmi healthy range is plausible`() {
        val reading = (BmiCalculator.calculate(70.0, 175.0) as BmiCalculator.Result.Success).reading
        assertTrue(reading.healthyWeightMinKg < 70.0)
        assertTrue(reading.healthyWeightMaxKg > 70.0)
    }

    @Test
    fun `bmi validation`() {
        assertTrue(BmiCalculator.calculate(-1.0, 175.0) is BmiCalculator.Result.Failure)
        assertTrue(BmiCalculator.calculate(70.0, 0.0) is BmiCalculator.Result.Failure)
    }

    // ---------- Date difference ----------

    @Test
    fun `same year diff`() {
        val r = DateDifferenceTool.between("2026-01-01", "2026-01-31")
        assertEquals(30L, (r as DateDifferenceTool.Result.Success).diff.days)
    }

    @Test
    fun `leap year counted`() {
        val r = DateDifferenceTool.between("2024-01-01", "2025-01-01")
        assertEquals(366L, (r as DateDifferenceTool.Result.Success).diff.days)
    }

    @Test
    fun `weekdays exclude weekends`() {
        // 2026-01-05 is Monday -> 2026-01-09 Friday: 5 weekdays between exclusive end? days=4
        val r = DateDifferenceTool.between("2026-01-05", "2026-01-12")
        val d = (r as DateDifferenceTool.Result.Success).diff
        assertEquals(7L, d.days)
        assertEquals(5L, d.weekdays) // Mon-Fri within, end Monday excluded
    }

    @Test
    fun `add days works forward and backward`() {
        val fwd = DateDifferenceTool.addDays("2026-10-19", 45)
        assertEquals(45L, (fwd as DateDifferenceTool.Result.Success).diff.days)
        val bwd = DateDifferenceTool.addDays("2026-10-19", -45)
        assertEquals(45L, (bwd as DateDifferenceTool.Result.Success).diff.days)
    }

    @Test
    fun `bad date format fails`() {
        assertTrue(DateDifferenceTool.between("19/10/2026", "2026-12-01") is DateDifferenceTool.Result.Failure)
        assertTrue(DateDifferenceTool.between("2026-13-40", "2026-12-01") is DateDifferenceTool.Result.Failure)
    }
}
