package com.drs.nexus.core.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeneratorToolsTest {

    private val uuid = UuidTool()
    private val pw = PasswordGenerator()

    @Test
    fun `uuid v4 correct format`() {
        val u = uuid.generate()[0]
        assertEquals(36, u.length)
        assertEquals(5, u.split("-").size)
        assertTrue(Regex("^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$").matches(u))
    }

    @Test
    fun `uuid version detection`() {
        assertEquals(4, uuid.version(uuid.generate()[0]))
        assertNull(uuid.version("not-a-uuid"))
    }

    @Test
    fun `uuid options uppercase no hyphens braces`() {
        val u = uuid.generate(1, UuidTool.UuidOptions(uppercase = true, withHyphens = false, braces = true))[0]
        assertEquals(34, u.length) // 32 hex chars + braces
        assertEquals(u, u.uppercase())
        assertTrue(u.startsWith("{") && u.endsWith("}"))
    }

    @Test
    fun `uuid unique in bulk`() {
        val list = uuid.generate(100)
        assertEquals(100, list.toSet().size)
    }

    @Test
    fun `password length respected`() {
        listOf(8, 12, 16, 32).forEach { len ->
            assertEquals(len, pw.generate(PasswordGenerator.Options(length = len)).length)
        }
    }

    @Test
    fun `password contains required classes`() {
        val opts = PasswordGenerator.Options(length = 24, uppercase = true, lowercase = true, digits = true, symbols = true)
        val p = pw.generate(opts)
        assertTrue(p.any { it.isUpperCase() })
        assertTrue(p.any { it.isLowerCase() })
        assertTrue(p.any { it.isDigit() })
        assertTrue(p.any { !it.isLetterOrDigit() })
    }

    @Test
    fun `password digits only when configured`() {
        val p = pw.generate(PasswordGenerator.Options(length = 10, uppercase = false, lowercase = false, digits = true, symbols = false))
        assertTrue(Regex("^\\d{10}$").matches(p))
    }

    @Test
    fun `password exclude ambiguous`() {
        repeat(20) {
            val p = pw.generate(PasswordGenerator.Options(length = 40, excludeAmbiguous = true))
            assertFalse(p.any { it in "Il1O0" })
        }
    }

    @Test
    fun `entropy and strength`() {
        val opts = PasswordGenerator.Options(length = 20)
        val bits = pw.entropyBits(opts)
        assertTrue(bits > 100)
        assertEquals(PasswordGenerator.Strength.VERY_STRONG, pw.strength(bits))
        assertTrue(pw.strength(10) == PasswordGenerator.Strength.WEAK)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `password with no classes throws`() {
        pw.generate(PasswordGenerator.Options(uppercase = false, lowercase = false, digits = false, symbols = false))
    }
}
