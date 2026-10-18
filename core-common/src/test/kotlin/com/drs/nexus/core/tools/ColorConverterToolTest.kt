package com.drs.nexus.core.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorConverterToolTest {

    private val tool = ColorConverterTool()

    @Test
    fun `6-digit hex parses to rgb and hsl`() {
        val r = tool.parse("#FF8000") as ColorConverterTool.ParseResult.Success
        assertEquals(255, r.info.rgb.red)
        assertEquals(128, r.info.rgb.green)
        assertEquals(0, r.info.rgb.blue)
        assertEquals(255, r.info.rgb.alpha)
        assertEquals(30.1176, r.info.hsl.hue, 0.01)
        assertEquals(100.0, r.info.hsl.saturation, 0.01)
        assertEquals(50.0, r.info.hsl.lightness, 0.01)
    }

    @Test
    fun `3-digit hex expands`() {
        val r = tool.parse("#0AF") as ColorConverterTool.ParseResult.Success
        assertEquals(0, r.info.rgb.red)
        assertEquals(170, r.info.rgb.green)
        assertEquals(255, r.info.rgb.blue)
    }

    @Test
    fun `8-digit hex keeps alpha`() {
        val r = tool.parse("80FF8000") as ColorConverterTool.ParseResult.Success
        assertEquals(128, r.info.rgb.alpha)
        assertEquals(255, r.info.rgb.red)
    }

    @Test
    fun `css outputs are well formed`() {
        val r = tool.parse("#FF8000") as ColorConverterTool.ParseResult.Success
        assertEquals("rgb(255, 128, 0)", r.info.cssRgb)
        assertEquals("hsl(30, 100%, 50%)", r.info.cssHsl)
        assertEquals("#FFFF8000", r.info.hexLong)
    }

    @Test
    fun `white suggests black text and black suggests white text`() {
        val white = tool.parse("#FFFFFF") as ColorConverterTool.ParseResult.Success
        val black = tool.parse("#000000") as ColorConverterTool.ParseResult.Success
        assertEquals("#000000", white.info.bestTextColor)
        assertEquals("#FFFFFF", black.info.bestTextColor)
    }

    @Test
    fun `hsl round trip preserves color`() {
        val r = tool.parse("#3366CC") as ColorConverterTool.ParseResult.Success
        val back = tool.hslToRgb(r.info.hsl.hue, r.info.hsl.saturation, r.info.hsl.lightness)
        assertEquals(r.info.rgb.red, back.red)
        assertEquals(r.info.rgb.green, back.green)
        assertEquals(r.info.rgb.blue, back.blue)
    }

    @Test
    fun `invalid input fails with message`() {
        assertTrue(tool.parse("#GGHHII") is ColorConverterTool.ParseResult.Failure)
        assertTrue(tool.parse("#12345") is ColorConverterTool.ParseResult.Failure)
        assertTrue(tool.parse("") is ColorConverterTool.ParseResult.Failure)
    }

    @Test
    fun `fromRgb validates channel range`() {
        assertTrue(tool.fromRgb(300, 0, 0) is ColorConverterTool.ParseResult.Failure)
        val ok = tool.fromRgb(10, 20, 30) as ColorConverterTool.ParseResult.Success
        assertEquals("#0A141E", ok.info.hex)
    }
}
