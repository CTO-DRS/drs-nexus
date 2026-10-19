package com.drs.nexus.core.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlParserToolTest {

    private val tool = UrlParserTool()

    private fun ok(url: String): UrlParserTool.ParsedUrl =
        (tool.parse(url) as UrlParserTool.Result.Success).parsed

    @Test
    fun `basic https url with default port`() {
        val p = ok("https://example.com/path/to/page?q=nexus&x=1#sec")
        assertEquals("https", p.scheme)
        assertEquals("example.com", p.host)
        assertEquals(443, p.port)
        assertTrue(p.isDefaultPort)
        assertEquals("/path/to/page", p.path)
        assertEquals(listOf("path", "to", "page"), p.pathSegments)
        assertEquals("q=nexus&x=1", p.query)
        assertEquals("sec", p.fragment)
    }

    @Test
    fun `http defaults to port 80`() {
        val p = ok("http://example.com")
        assertEquals(80, p.port)
        assertTrue(p.isDefaultPort)
        assertEquals("", p.path)
    }

    @Test
    fun `explicit port preserved and not default`() {
        val p = ok("https://example.com:8443/api")
        assertEquals(8443, p.port)
        assertFalse(p.isDefaultPort)
        assertEquals(8443, p.port)
    }

    @Test
    fun `query params decoded including plus and percent`() {
        val p = ok("https://a.io/s?q=hello%20world&tag=a+b")
        assertEquals(2, p.params.size)
        assertEquals("hello world", p.params[0].value)
        assertEquals("a b", p.params[1].value)
    }

    @Test
    fun `param without value becomes empty`() {
        val p = ok("https://a.io/?flag&x=2")
        assertEquals(2, p.params.size)
        assertEquals("flag", p.params[0].key)
        assertEquals("", p.params[0].value)
        assertEquals("2", p.params[1].value)
    }

    @Test
    fun `user info extracted`() {
        val p = ok("https://user:pass@example.com/")
        assertEquals("user:pass", p.userInfo)
        assertEquals("example.com", p.host)
    }

    @Test
    fun `missing scheme rejected`() {
        val r = tool.parse("example.com/x")
        assertTrue(r is UrlParserTool.Result.Failure)
        assertTrue((r as UrlParserTool.Result.Failure).message.contains("scheme"))
    }

    @Test
    fun `empty and spaced inputs rejected`() {
        assertTrue(tool.parse("") is UrlParserTool.Result.Failure)
        assertTrue(tool.parse("  ") is UrlParserTool.Result.Failure)
        assertTrue(tool.parse("http://exa mple.com") is UrlParserTool.Result.Failure)
    }

    @Test
    fun `malformed url caught with message`() {
        val r = tool.parse("http://[::1")
        assertTrue(r is UrlParserTool.Result.Failure)
        assertTrue((r as UrlParserTool.Result.Failure).message.contains("Malformed"))
    }

    @Test
    fun `normalize round trip preserves full url`() {
        val url = "https://example.com:8443/a/b?q=1&z=2#f"
        assertEquals(url, tool.normalize(ok(url)))
    }

    @Test
    fun `normalize drops default port`() {
        assertEquals("https://example.com/x", tool.normalize(ok("https://example.com:443/x")))
    }

    @Test
    fun `format result is scheme host path`() {
        assertEquals("https://example.com/a", tool.formatResult(ok("https://example.com/a?q=9")))
    }
}
