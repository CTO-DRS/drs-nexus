package com.drs.nexus.core.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EncodingToolsTest {

    private val b64 = Base64Tool()
    private val hash = HashTool()
    private val url = UrlTool()

    @Test
    fun `base64 round trip ascii`() {
        val enc = b64.encode("Hello, DRS Nexus!")
        assertEquals("SGVsbG8sIERSUyBOZXh1cyE=", enc)
        val dec = b64.decode(enc)
        assertTrue(dec is Base64Tool.Base64Result.Success)
        assertEquals("Hello, DRS Nexus!", (dec as Base64Tool.Base64Result.Success).text)
    }

    @Test
    fun `base64 round trip utf8 arabic`() {
        val text = "مرحبا بالعالم"
        val dec = b64.decode(b64.encode(text))
        assertEquals(text, (dec as Base64Tool.Base64Result.Success).text)
    }

    @Test
    fun `base64 urlsafe variant`() {
        val enc = b64.encode("subjects?_ill", urlSafe = true)
        assertFalse(enc.contains('+') || enc.contains('/'))
    }

    @Test
    fun `base64 without padding`() {
        val enc = b64.encode("ab", withPadding = false)
        assertFalse(enc.endsWith("="))
    }

    @Test
    fun `base64 invalid input fails`() {
        assertTrue(b64.decode("!!!!") is Base64Tool.Base64Result.Failure)
        assertTrue(b64.decode("") is Base64Tool.Base64Result.Failure)
    }

    @Test
    fun `sha256 known vector`() {
        // echo -n "hello" | sha256sum
        assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", hash.hash("hello", HashTool.Algorithm.SHA_256))
    }

    @Test
    fun `sha1 known vector`() {
        assertEquals("aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d", hash.hash("hello", HashTool.Algorithm.SHA_1))
    }

    @Test
    fun `md5 known vector`() {
        assertEquals("5d41402abc4b2a76b9719d911017c592", hash.hash("hello", HashTool.Algorithm.MD5))
    }

    @Test
    fun `sha512 length and verify`() {
        val h = hash.hash("hello", HashTool.Algorithm.SHA_512)
        assertEquals(128, h.length)
        assertTrue(hash.verify("hello", h.uppercase(), HashTool.Algorithm.SHA_512))
        assertFalse(hash.verify("world", h, HashTool.Algorithm.SHA_512))
    }

    @Test
    fun `hashAll returns four algorithms`() {
        val all = hash.hashAll("x")
        assertEquals(4, all.size)
        all.values.forEach { assertTrue(it.isNotEmpty()) }
    }

    @Test
    fun `url encode decode round trip`() {
        val src = "https://example.com/path?a=1&b=مرحبا world"
        assertEquals(src, (url.decode(url.encode(src)) as UrlTool.UrlResult.Success).text)
    }

    @Test
    fun `url encode space handling`() {
        assertTrue(url.encode("a b").contains('+'))
        assertTrue(url.encode("a b", plusForSpace = false).contains("%20"))
    }

    @Test
    fun `url decode invalid fails`() {
        assertTrue(url.decode("%ZZ") is UrlTool.UrlResult.Failure)
    }
}
