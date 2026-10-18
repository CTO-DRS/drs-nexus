package com.drs.nexus.core.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BaseConverterToolTest {

    private val tool = BaseConverterTool()

    @Test
    fun `decimal 255 converts to all quick bases`() {
        val r = tool.convert(255L, 10, 36)
        assertEquals("11111111", r.binary)
        assertEquals("377", r.octal)
        assertEquals("255", r.decimal)
        assertEquals("FF", r.hex)
        assertEquals("73", r.customBase) // 255 in base 36
    }

    @Test
    fun `binary input parses correctly`() {
        val p = tool.parse("1010", 2)
        assertTrue(p is BaseConverterTool.ParseResult.Success)
        assertEquals(10L, (p as BaseConverterTool.ParseResult.Success).value)
    }

    @Test
    fun `hex ffff parses to 65535`() {
        val p = tool.parse("FFFF", 16)
        assertEquals(65535L, (p as BaseConverterTool.ParseResult.Success).value)
    }

    @Test
    fun `invalid digit rejected for base`() {
        val p = tool.parse("129", 8)
        assertTrue(p is BaseConverterTool.ParseResult.Failure)
    }

    @Test
    fun `negative numbers supported`() {
        val p = tool.parse("-101", 2)
        assertEquals(-5L, (p as BaseConverterTool.ParseResult.Success).value)
    }

    @Test
    fun `base bounds enforced`() {
        assertTrue(tool.parse("1", 1) is BaseConverterTool.ParseResult.Failure)
        assertTrue(tool.parse("1", 37) is BaseConverterTool.ParseResult.Failure)
        assertTrue(tool.parse("", 10) is BaseConverterTool.ParseResult.Failure)
    }

    @Test
    fun `isValidInBase accepts uppercase and lowercase`() {
        assertTrue(BaseConverterTool.isValidInBase("aF", 16))
        assertTrue(BaseConverterTool.isValidInBase("777", 8))
        assertFalse(BaseConverterTool.isValidInBase("8", 8))
        assertFalse(BaseConverterTool.isValidInBase("Z", 35)) // Z is index 35, needs base 36
        assertTrue(BaseConverterTool.isValidInBase("Z", 36))
    }

    @Test
    fun `large 64-bit value round-trips`() {
        val v = Long.MAX_VALUE
        val r = tool.convert(v, 10, 2)
        assertEquals(v, r.binary.toLong(2))
    }
}
