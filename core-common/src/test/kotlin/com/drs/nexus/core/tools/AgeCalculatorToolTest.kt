package com.drs.nexus.core.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class AgeCalculatorToolTest {

    private val tool = AgeCalculatorTool()

    private fun ok(b: String, t: String): AgeCalculatorTool.AgeResult =
        (tool.compute(LocalDate.parse(b), LocalDate.parse(t)) as AgeCalculatorTool.Result.Success).age

    @Test
    fun `exact decomposition years months days`() {
        val a = ok("1990-05-10", "2020-06-15")
        assertEquals(30L, a.years)
        assertEquals(1L, a.months)
        assertEquals(5L, a.days)
    }

    @Test
    fun `birthday today yields zero countdown`() {
        val a = ok("1990-05-10", "2020-05-10")
        assertEquals(30L, a.years)
        assertTrue(a.isBirthdayToday)
        assertEquals(0L, a.nextBirthdayInDays)
    }

    @Test
    fun `next birthday date and countdown`() {
        val a = ok("1990-05-10", "2020-06-15")
        assertEquals(LocalDate.of(2021, 5, 10), a.nextBirthdayDate)
        assertEquals(329L, a.nextBirthdayInDays)
        assertFalse(a.isBirthdayToday)
    }

    @Test
    fun `feb 29 birthday celebrated on feb 28 in common years`() {
        val a = ok("2000-02-29", "2026-02-28")
        assertEquals(26L, a.years)
        assertTrue(a.isBirthdayToday)
    }

    @Test
    fun `totals are consistent`() {
        val a = ok("2000-01-15", "2010-03-20")
        assertEquals(122L, a.totalMonths) // 10y*12 + 2
        assertEquals(a.totalDays, a.totalWeeks * 7 + (a.totalDays % 7))
        assertTrue(a.totalDays > 3000)
    }

    @Test
    fun `weekday of birth is correct`() {
        assertEquals(DayOfWeek.SATURDAY, ok("2000-01-01", "2020-01-01").birthDayOfWeek)
        assertEquals(DayOfWeek.SATURDAY, ok("2000-01-15", "2020-01-15").birthDayOfWeek)
    }

    @Test
    fun `same day birth yields newborn zeros`() {
        val a = ok("2000-01-15", "2000-01-15")
        assertEquals(0L, a.years)
        assertEquals(0L, a.months)
        assertEquals(0L, a.days)
        assertTrue(a.isBirthdayToday)
    }

    @Test
    fun `month end clamping stays calendar exact`() {
        val a = ok("1990-01-31", "1990-03-01")
        assertEquals(0L, a.years)
        assertEquals(1L, a.months)
        assertEquals(1L, a.days)
    }

    @Test
    fun `future birth date fails`() {
        val r = tool.compute(LocalDate.parse("2030-01-01"), LocalDate.parse("2020-01-01"))
        assertTrue(r is AgeCalculatorTool.Result.Failure)
        assertTrue((r as AgeCalculatorTool.Result.Failure).message.contains("future"))
    }

    @Test
    fun `format result is compact`() {
        assertEquals("30y 1m 5d", tool.formatResult(ok("1990-05-10", "2020-06-15")))
    }
}
