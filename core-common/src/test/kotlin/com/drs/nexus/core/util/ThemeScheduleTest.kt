package com.drs.nexus.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class ThemeScheduleTest {

    // ---------- parseHm ----------

    @Test
    fun `parseHm accepts valid 24h values`() {
        assertEquals(0, ThemeSchedule.parseHm("00:00"))
        assertEquals(19 * 60, ThemeSchedule.parseHm("19:00"))
        assertEquals(23 * 60 + 59, ThemeSchedule.parseHm("23:59"))
        assertEquals(6 * 60 + 5, ThemeSchedule.parseHm("6:05"))
        assertEquals(6 * 60 + 5, ThemeSchedule.parseHm(" 6:05 "))
    }

    @Test
    fun `parseHm rejects malformed values`() {
        assertNull(ThemeSchedule.parseHm(""))
        assertNull(ThemeSchedule.parseHm("19"))
        assertNull(ThemeSchedule.parseHm("19:60"))
        assertNull(ThemeSchedule.parseHm("24:00"))
        assertNull(ThemeSchedule.parseHm("-1:30"))
        assertNull(ThemeSchedule.parseHm("19:0x"))
        assertNull(ThemeSchedule.parseHm("7pm"))
    }

    @Test
    fun `formatHm pads to canonical HHmm`() {
        assertEquals("00:00", ThemeSchedule.formatHm(0))
        assertEquals("06:00", ThemeSchedule.formatHm(360))
        assertEquals("19:30", ThemeSchedule.formatHm(19 * 60 + 30))
        assertEquals("23:59", ThemeSchedule.formatHm(24 * 60 - 1))
        assertEquals("23:59", ThemeSchedule.formatHm(24 * 60 + 99)) // clamped
    }

    // ---------- inWindow: same-day window ----------

    @Test
    fun `same-day window matches inside and rejects outside`() {
        val from = 9 * 60      // 09:00
        val to = 17 * 60       // 17:00
        assertTrue(ThemeSchedule.inWindow(9 * 60, from, to))        // start inclusive
        assertTrue(ThemeSchedule.inWindow(12 * 60, from, to))       // mid-day
        assertTrue(ThemeSchedule.inWindow(16 * 60 + 59, from, to))  // last minute
        assertFalse(ThemeSchedule.inWindow(17 * 60, from, to))      // end exclusive
        assertFalse(ThemeSchedule.inWindow(8 * 60 + 59, from, to))
        assertFalse(ThemeSchedule.inWindow(23 * 60, from, to))
    }

    // ---------- inWindow: overnight window ----------

    @Test
    fun `overnight window spans midnight`() {
        val from = 19 * 60     // 19:00
        val to = 6 * 60        // 06:00
        assertTrue(ThemeSchedule.inWindow(19 * 60, from, to))       // evening start
        assertTrue(ThemeSchedule.inWindow(23 * 59, from, to))       // late night
        assertTrue(ThemeSchedule.inWindow(0, from, to))             // just after midnight
        assertTrue(ThemeSchedule.inWindow(5 * 60 + 59, from, to))   // early morning
        assertFalse(ThemeSchedule.inWindow(6 * 60, from, to))       // morning end exclusive
        assertFalse(ThemeSchedule.inWindow(12 * 60, from, to))      // noon
    }

    @Test
    fun `empty window means dark all day`() {
        for (minute in intArrayOf(0, 360, 720, 1140, 1439)) {
            assertTrue(ThemeSchedule.inWindow(minute, 540, 540))
        }
    }

    // ---------- isDarkNow ----------

    private fun millisAt(hour: Int, minute: Int, tz: TimeZone): Long {
        val cal = Calendar.getInstance(tz).apply {
            set(2026, Calendar.OCTOBER, 18, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    @Test
    fun `isDarkNow uses provided timezone`() {
        val tz = TimeZone.getTimeZone("UTC")
        assertTrue(ThemeSchedule.isDarkNow(millisAt(20, 0, tz), "19:00", "06:00", tz))
        assertFalse(ThemeSchedule.isDarkNow(millisAt(12, 0, tz), "19:00", "06:00", tz))
    }

    @Test
    fun `isDarkNow falls back to defaults on invalid stored values`() {
        val tz = TimeZone.getTimeZone("UTC")
        // invalid from/to -> defaults 19:00-06:00
        assertTrue(ThemeSchedule.isDarkNow(millisAt(21, 0, tz), "bad", "xx", tz))
        assertFalse(ThemeSchedule.isDarkNow(millisAt(10, 0, tz), "bad", "xx", tz))
    }

    @Test
    fun `isDarkNow honors exact boundaries`() {
        val tz = TimeZone.getTimeZone("UTC")
        assertTrue(ThemeSchedule.isDarkNow(millisAt(19, 0, tz), "19:00", "06:00", tz))
        assertFalse(ThemeSchedule.isDarkNow(millisAt(6, 0, tz), "19:00", "06:00", tz))
    }
}
