package com.drs.nexus.core.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UtilityToolsTest {

    private val text = TextUtilitiesTool()
    private val regex = RegexTesterTool()
    private val ts = TimestampConverterTool()
    private val conv = UnitConverterTool()
    private val lorem = LoremIpsumTool()

    @Test
    fun `text stats counts correctly`() {
        val s = text.stats("Hello world. This is DRS.\n\nSecond paragraph.")
        assertEquals(7, s.words)
        assertTrue(s.paragraphs == 2)
        assertTrue(s.sentences == 3)
        assertTrue(s.characters > 0 && s.words > 0)
    }

    @Test
    fun `case conversions`() {
        assertEquals("helloWorldExample", text.convertCase("Hello World Example", TextUtilitiesTool.CaseStyle.CAMEL))
        assertEquals("HelloWorldExample", text.convertCase("hello world example", TextUtilitiesTool.CaseStyle.PASCAL))
        assertEquals("hello_world", text.convertCase("Hello World", TextUtilitiesTool.CaseStyle.SNAKE))
        assertEquals("hello-world", text.convertCase("Hello World", TextUtilitiesTool.CaseStyle.KEBAB))
        assertEquals("HELLO WORLD", text.convertCase("hello world", TextUtilitiesTool.CaseStyle.UPPER))
        assertEquals("Hello World", text.convertCase("HELLO WORLD", TextUtilitiesTool.CaseStyle.TITLE))
    }

    @Test
    fun `reverse and space cleanup`() {
        assertEquals("cba", text.reverse("abc"))
        assertEquals("a b c", text.removeExtraSpaces("a    b \n c"))
        assertEquals("x\ny", text.trimLines("  x  \n   y  ", removeEmpty = true))
    }

    @Test
    fun `arabic text preserved in case conversion`() {
        assertEquals("مرحبا_بالعالم", text.convertCase("مرحبا بالعالم", TextUtilitiesTool.CaseStyle.SNAKE))
    }

    @Test
    fun `regex finds matches with positions`() {
        val r = regex.test("\\d+", "abc 123 def 456")
        assertTrue(r is RegexTesterTool.RegexResult.Success)
        val s = r as RegexTesterTool.RegexResult.Success
        assertEquals(2, s.matchCount)
        assertEquals(4, s.matches[0].start)
        assertEquals("123", s.matches[0].value)
    }

    @Test
    fun `regex replacement preview`() {
        val r = regex.test("cat", "cat and cat", replacement = "dog")
        assertTrue(r is RegexTesterTool.RegexResult.Success)
        assertEquals("dog and dog", (r as RegexTesterTool.RegexResult.Success).replacePreview)
    }

    @Test
    fun `regex case insensitive flag`() {
        val r = regex.test("hello", "HELLO world", caseInsensitive = true)
        assertEquals(1, (r as RegexTesterTool.RegexResult.Success).matchCount)
    }

    @Test
    fun `regex invalid pattern fails`() {
        assertTrue(regex.test("[unclosed", "x") is RegexTesterTool.RegexResult.Failure)
    }

    @Test
    fun `epoch to date conversion`() {
        val r = ts.fromEpoch("0", "UTC")
        assertTrue(r is TimestampConverterTool.TsResult.Success)
        assertTrue(((r as TimestampConverterTool.TsResult.Success).text).startsWith("1970-01-01"))
    }

    @Test
    fun `epoch millis auto detection`() {
        val r = ts.fromEpoch("1000000000000", "UTC") // 2001-09-09 in millis
        assertTrue(r is TimestampConverterTool.TsResult.Success)
        assertTrue(((r as TimestampConverterTool.TsResult.Success).text).startsWith("2001-09-09"))
    }

    @Test
    fun `epoch invalid input fails`() {
        assertTrue(ts.fromEpoch("abc") is TimestampConverterTool.TsResult.Failure)
        assertTrue(ts.fromEpoch("") is TimestampConverterTool.TsResult.Failure)
    }

    @Test
    fun `now returns plausible epoch`() {
        val now = ts.now()
        assertTrue(now > 1_600_000_000L)
    }

    @Test
    fun `unit conversion length`() {
        assertEquals(1000.0, (conv.convert(1.0, UnitConverterTool.Unit.KM, UnitConverterTool.Unit.M) as UnitConverterTool.ConvResult.Success).value, 0.0001)
        assertEquals(2.54, (conv.convert(1.0, UnitConverterTool.Unit.IN, UnitConverterTool.Unit.CM) as UnitConverterTool.ConvResult.Success).value, 0.0001)
    }

    @Test
    fun `unit conversion temperature`() {
        assertEquals(100.0, (conv.convert(212.0, UnitConverterTool.Unit.F, UnitConverterTool.Unit.C) as UnitConverterTool.ConvResult.Success).value, 0.0001)
        assertEquals(273.15, (conv.convert(0.0, UnitConverterTool.Unit.C, UnitConverterTool.Unit.K) as UnitConverterTool.ConvResult.Success).value, 0.0001)
    }

    @Test
    fun `unit conversion data`() {
        assertEquals(1024.0, (conv.convert(1.0, UnitConverterTool.Unit.KB, UnitConverterTool.Unit.B) as UnitConverterTool.ConvResult.Success).value, 0.0001)
    }

    @Test
    fun `cross category rejected`() {
        assertTrue(conv.convert(1.0, UnitConverterTool.Unit.M, UnitConverterTool.Unit.KG) is UnitConverterTool.ConvResult.Failure)
    }

    @Test
    fun `lorem generates paragraphs sentences words`() {
        val p = lorem.generate(LoremIpsumTool.Mode.PARAGRAPHS, 3)
        assertEquals(3, p.split("\n\n").size)
        val s = lorem.generate(LoremIpsumTool.Mode.SENTENCES, 4)
        assertEquals(4, Regex("\\.").findAll(s).count())
        val w = lorem.generate(LoremIpsumTool.Mode.WORDS, 10, startWithLorem = false)
        assertEquals(10, w.split(" ").size)
    }
}
