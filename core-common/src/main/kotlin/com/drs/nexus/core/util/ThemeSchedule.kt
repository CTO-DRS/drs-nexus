package com.drs.nexus.core.util

import java.util.Calendar
import java.util.TimeZone

/**
 * Pure, testable engine behind the "scheduled dark mode" feature.
 *
 * A user picks a daily window (e.g. 19:00 -> 06:00) during which the app forces
 * dark theme. Windows that cross midnight are fully supported; an empty window
 * (from == to) is interpreted as "dark all day".
 */
object ThemeSchedule {

    /**
     * Parses a strict "HH:mm" 24-hour string into minutes-of-day.
     * Returns null for anything malformed or out of range.
     */
    fun parseHm(value: String): Int? {
        val trimmed = value.trim()
        val match = Regex("^([0-9]{1,2}):([0-9]{2})$").find(trimmed) ?: return null
        val (hRaw, mRaw) = match.destructured
        val h = hRaw.toIntOrNull() ?: return null
        val m = mRaw.toIntOrNull() ?: return null
        if (h !in 0..23 || m !in 0..59) return null
        return h * 60 + m
    }

    /** Normalizes a parsed value back into a canonical zero-padded "HH:mm". */
    fun formatHm(minutesOfDay: Int): String {
        val clamped = minutesOfDay.coerceIn(0, 24 * 60 - 1)
        return "%02d:%02d".format(clamped / 60, clamped % 60)
    }

    /**
     * True when [nowMinutes] is inside [fromMinutes]..[toMinutes] (exclusive end).
     * Same-day window: from 09:00 to 17:00 matches 09:00..16:59.
     * Overnight window: from 19:00 to 06:00 matches 19:00..23:59 and 00:00..05:59.
     * Empty window (from == to) means "always dark".
     */
    fun inWindow(nowMinutes: Int, fromMinutes: Int, toMinutes: Int): Boolean {
        val now = nowMinutes.coerceIn(0, 24 * 60 - 1)
        return when {
            fromMinutes == toMinutes -> true
            fromMinutes < toMinutes -> now >= fromMinutes && now < toMinutes
            else -> now >= fromMinutes || now < toMinutes
        }
    }

    /**
     * Full decision for scheduled mode: current wall-clock time in [tz] is
     * checked against the user's chosen window. Invalid stored values fall
     * back to the defaults (19:00 -> 06:00).
     */
    fun isDarkNow(
        nowMillis: Long,
        fromHm: String,
        toHm: String,
        tz: TimeZone = TimeZone.getDefault()
    ): Boolean {
        val from = parseHm(fromHm) ?: parseHm(DEFAULT_FROM) ?: 19 * 60
        val to = parseHm(toHm) ?: parseHm(DEFAULT_TO) ?: 6 * 60
        val cal = Calendar.getInstance(tz).apply { timeInMillis = nowMillis }
        val nowMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        return inWindow(nowMinutes, from, to)
    }

    const val DEFAULT_FROM = "19:00"
    const val DEFAULT_TO = "06:00"
}
