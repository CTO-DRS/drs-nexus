package com.drs.nexus.core.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class JwtDecoderToolTest {

    private val tool = JwtDecoderTool()

    private fun b64url(s: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(s.toByteArray())

    private val header = """{"alg":"HS256","typ":"JWT"}"""
    private val payload = """{"sub":"user-42","iss":"drs.example","aud":"nexus-app","iat":1600000000,"exp":2000000000}"""

    private fun token(expired: Boolean = false): String {
        val p = if (expired) payload.replace("2000000000", "1000000000") else payload
        return "${b64url(header)}.${b64url(p)}.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
    }

    @Test
    fun `decodes header algorithm and pretty json`() {
        val r = tool.decode(token(), nowEpochSeconds = 1_700_000_000) as JwtDecoderTool.JwtResult.Success
        assertEquals("HS256", r.result.algorithm)
        assertTrue(r.result.header.prettyJson.contains("\"alg\""))
        assertTrue(r.result.payload.prettyJson.contains("user-42"))
    }

    @Test
    fun `claims extracted`() {
        val r = tool.decode(token(), nowEpochSeconds = 1_700_000_000) as JwtDecoderTool.JwtResult.Success
        val c = r.result.claims
        assertEquals("user-42", c.subject)
        assertEquals("drs.example", c.issuer)
        assertEquals("nexus-app", c.audience)
        assertEquals(1600000000L, c.issuedAtEpoch)
        assertEquals(2000000000L, c.expiresAtEpoch)
    }

    @Test
    fun `valid token reports VALID with seconds left`() {
        val r = tool.decode(token(), nowEpochSeconds = 1_700_000_000) as JwtDecoderTool.JwtResult.Success
        assertEquals(JwtDecoderTool.ExpiryStatus.VALID, r.result.status)
        assertEquals(300_000_000L, r.result.secondsLeft)
    }

    @Test
    fun `expired token reports EXPIRED`() {
        val r = tool.decode(token(expired = true), nowEpochSeconds = 1_700_000_000) as JwtDecoderTool.JwtResult.Success
        assertEquals(JwtDecoderTool.ExpiryStatus.EXPIRED, r.result.status)
        assertTrue(r.result.secondsLeft!! < 0)
    }

    @Test
    fun `token without exp reports NO_EXPIRY`() {
        val p = """{"sub":"abc"}"""
        val r = tool.decode("${b64url(header)}.${b64url(p)}.sig", 1_700_000_000) as JwtDecoderTool.JwtResult.Success
        assertEquals(JwtDecoderTool.ExpiryStatus.NO_EXPIRY, r.result.status)
    }

    @Test
    fun `wrong section count fails`() {
        assertTrue(tool.decode("a.b") is JwtDecoderTool.JwtResult.Failure)
        assertTrue(tool.decode("") is JwtDecoderTool.JwtResult.Failure)
    }

    @Test
    fun `non base64 payload fails gracefully`() {
        assertTrue(tool.decode("!!!.???.sig") is JwtDecoderTool.JwtResult.Failure)
    }

    @Test
    fun `payload that is not json object fails`() {
        val notObject = b64url("[1,2,3]")
        val r = tool.decode("${b64url(header)}.${notObject}.sig")
        assertTrue(r is JwtDecoderTool.JwtResult.Failure)
    }
}
