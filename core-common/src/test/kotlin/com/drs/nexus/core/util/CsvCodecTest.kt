package com.drs.nexus.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class CsvCodecTest {

    @Test
    fun `plain field stays unquoted`() {
        assertEquals("nexus", CsvCodec.escape("nexus"))
        assertEquals("", CsvCodec.escape(""))
    }

    @Test
    fun `field with comma is quoted`() {
        assertEquals("\"a,b\"", CsvCodec.escape("a,b"))
    }

    @Test
    fun `quotes are doubled inside quoted field`() {
        assertEquals("\"say \"\"hi\"\"\"", CsvCodec.escape("say \"hi\""))
    }

    @Test
    fun `newline and carriage return force quoting`() {
        assertEquals("\"line1\nline2\"", CsvCodec.escape("line1\nline2"))
        assertEquals("\"a\rb\"", CsvCodec.escape("a\rb"))
    }

    @Test
    fun `unicode and arabic pass through untouched`() {
        assertEquals("محرك الدعم", CsvCodec.escape("محرك الدعم"))
        assertEquals("100%", CsvCodec.escape("100%"))
    }

    @Test
    fun `line joins fields with commas`() {
        assertEquals("a,b,\"c,d\"", CsvCodec.line("a", "b", "c,d"))
    }

    @Test
    fun `document uses CRLF and ends with complete line`() {
        val doc = CsvCodec.document(
            listOf(
                listOf("tool_id", "output"),
                listOf("hash", "value, with comma")
            )
        )
        assertEquals("tool_id,output\r\nhash,\"value, with comma\"\r\n", doc)
    }

    @Test
    fun `bom is the utf8 zero width no-break space`() {
        assertEquals("\uFEFF", CsvCodec.BOM)
        assertEquals(1, CsvCodec.BOM.length)
    }
}
