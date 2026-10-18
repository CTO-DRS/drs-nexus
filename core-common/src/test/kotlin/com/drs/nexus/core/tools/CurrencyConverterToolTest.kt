package com.drs.nexus.core.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrencyConverterToolTest {

    private val tool = CurrencyConverterTool()

    @Test
    fun `usd to sar uses pegged rate`() {
        val r = tool.convert(100.0, "USD", "SAR") as CurrencyConverterTool.Result.Success
        assertEquals(375.0, r.conversion.result, 0.001)
        assertEquals(3.75, r.conversion.unitRate, 0.000001)
    }

    @Test
    fun `sar to usd inverts pegged rate`() {
        val r = tool.convert(375.0, "SAR", "USD") as CurrencyConverterTool.Result.Success
        assertEquals(100.0, r.conversion.result, 0.001)
    }

    @Test
    fun `cross rate eur to gbp is sane`() {
        val r = tool.convert(100.0, "EUR", "GBP") as CurrencyConverterTool.Result.Success
        // 100 EUR = 100/0.86 USD = 116.279 USD -> *0.745 = 86.63 GBP
        assertEquals(86.6279, r.conversion.result, 0.01)
    }

    @Test
    fun `round trip preserves value`() {
        val forth = tool.convert(250.0, "USD", "JPY") as CurrencyConverterTool.Result.Success
        val back = tool.convert(forth.conversion.result, "JPY", "USD") as CurrencyConverterTool.Result.Success
        assertEquals(250.0, back.conversion.result, 0.01)
    }

    @Test
    fun `zero amount converts to zero`() {
        val r = tool.convert(0.0, "USD", "EUR") as CurrencyConverterTool.Result.Success
        assertEquals(0.0, r.conversion.result, 0.0)
    }

    @Test
    fun `negative amount rejected`() {
        assertTrue(tool.convert(-5.0, "USD", "EUR") is CurrencyConverterTool.Result.Failure)
    }

    @Test
    fun `unknown currency codes fail with message`() {
        val r = tool.convert(1.0, "XXX", "USD") as CurrencyConverterTool.Result.Failure
        assertTrue(r.message.contains("XXX"))
        assertTrue(tool.convert(1.0, "USD", "xyz") is CurrencyConverterTool.Result.Failure)
    }

    @Test
    fun `lookup is case insensitive`() {
        assertEquals("SAR", CurrencyConverterTool.byCode("sar")?.code)
    }

    @Test
    fun `large JPY result rounded to 2 decimals`() {
        val r = tool.convert(1.0, "USD", "JPY") as CurrencyConverterTool.Result.Success
        // 151.8 * 1.0 -> rounds to hundredths: exactly 151.8
        assertEquals(151.8, r.conversion.result, 0.0001)
    }

    @Test
    fun `rates table has reference date and 20 plus currencies`() {
        assertTrue(CurrencyConverterTool.RATES_DATE.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
        assertTrue(CurrencyConverterTool.CURRENCIES.size >= 20)
    }
}
