package com.drs.nexus.core.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonFormatterToolTest {

    private val tool = JsonFormatterTool()

    @Test
    fun `formats nested object with indentation`() {
        val result = tool.format("""{"a":{"b":[1,2]}}""")
        assertTrue(result is JsonFormatterTool.JsonResult.Success)
        val s = result as JsonFormatterTool.JsonResult.Success
        assertTrue(s.formatted.contains("\n"))
        assertTrue(s.formatted.contains("  \"b\": ["))
    }

    @Test
    fun `minify removes whitespace`() {
        val s = tool.format("""{ "a" : 1 , "b" : "x" }""") as JsonFormatterTool.JsonResult.Success
        assertEquals("""{"a":1,"b":"x"}""", s.minified)
    }

    @Test
    fun `invalid json reports failure with position`() {
        val result = tool.format("""{"a": }""")
        assertTrue(result is JsonFormatterTool.JsonResult.Failure)
        val f = result as JsonFormatterTool.JsonResult.Failure
        assertTrue(f.line >= 1)
        assertTrue(f.column >= 1)
    }

    @Test
    fun `stats counts keys objects arrays depth`() {
        val s = tool.format("""{"a":1,"b":{"c":[1,2,{"d":3}]}}""") as JsonFormatterTool.JsonResult.Success
        assertEquals(4, s.stats.keys)
        assertEquals(3, s.stats.objects)
        assertEquals(1, s.stats.arrays)
        assertEquals(5, s.stats.maxDepth)
    }

    @Test
    fun `unicode content preserved`() {
        val s = tool.format("""{"مدينة": "الرياض"}""") as JsonFormatterTool.JsonResult.Success
        assertTrue(s.minified.contains("الرياض"))
        assertTrue(s.minified.contains("مدينة"))
    }

    @Test
    fun `empty input fails gracefully`() {
        assertTrue(tool.format("") is JsonFormatterTool.JsonResult.Failure)
        assertTrue(tool.format("   ") is JsonFormatterTool.JsonResult.Failure)
    }

    @Test
    fun `array top level`() {
        val s = tool.format("[1,2,3]") as JsonFormatterTool.JsonResult.Success
        assertTrue(s.minified == "[1,2,3]")
        assertEquals(1, s.stats.arrays)
    }
}
