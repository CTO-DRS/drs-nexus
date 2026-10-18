package com.drs.nexus.core.tools

import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * Real color converter: HEX <-> RGB <-> HSL.
 * Accepts #RGB, #RRGGBB and #AARRGGBB (also without '#'), outputs every representation
 * plus a YIQ-based black/white readable-text suggestion used by real design systems.
 */
class ColorConverterTool {

    data class Rgb(val red: Int, val green: Int, val blue: Int, val alpha: Int = 255)
    data class Hsl(val hue: Double, val saturation: Double, val lightness: Double)

    data class ColorInfo(
        val hex: String,
        val hexLong: String,
        val rgb: Rgb,
        val hsl: Hsl,
        val cssRgb: String,
        val cssHsl: String,
        val bestTextColor: String
    )

    sealed class ParseResult {
        data class Success(val info: ColorInfo) : ParseResult()
        data class Failure(val message: String) : ParseResult()
    }

    fun parse(input: String): ParseResult {
        val raw = input.trim().removePrefix("#").uppercase()
        if (raw.isEmpty()) return ParseResult.Failure("Color is empty")
        if (!raw.all { it in "0123456789ABCDEF" }) {
            return ParseResult.Failure("'$input' contains non-hex characters")
        }
        val (a, r, g, b) = when (raw.length) {
            3 -> {
                val chars = raw.map { "$it$it" }
                listOf("FF", chars[0], chars[1], chars[2])
            }
            6 -> listOf("FF", raw.substring(0, 2), raw.substring(2, 4), raw.substring(4, 6))
            8 -> listOf(raw.substring(0, 2), raw.substring(2, 4), raw.substring(4, 6), raw.substring(6, 8))
            else -> return ParseResult.Failure("Use #RGB, #RRGGBB or #AARRGGBB format")
        }
        val rgb = Rgb(r.toInt(16), g.toInt(16), b.toInt(16), a.toInt(16))
        return ParseResult.Success(buildInfo(rgb))
    }

    /** Converts from RGB channel values (e.g. coming from a picker). */
    fun fromRgb(red: Int, green: Int, blue: Int): ParseResult {
        val channels = listOf(red, green, blue)
        if (channels.any { it < 0 || it > 255 }) return ParseResult.Failure("Channels must be 0–255")
        return ParseResult.Success(buildInfo(Rgb(red, green, blue)))
    }

    private fun buildInfo(rgb: Rgb): ColorInfo {
        val hexLong = "#${rgb.alpha.toHex()}${rgb.red.toHex()}${rgb.green.toHex()}${rgb.blue.toHex()}"
        val hex = "#${rgb.red.toHex()}${rgb.green.toHex()}${rgb.blue.toHex()}"
        val hsl = rgbToHsl(rgb)
        return ColorInfo(
            hex = hex,
            hexLong = hexLong,
            rgb = rgb,
            hsl = hsl,
            cssRgb = "rgb(${rgb.red}, ${rgb.green}, ${rgb.blue})",
            cssHsl = "hsl(${hsl.hue.roundToInt()}, ${hsl.saturation.roundToInt()}%, ${hsl.lightness.roundToInt()}%)",
            bestTextColor = readableTextColor(rgb)
        )
    }

    private fun Int.toHex() = toString(16).uppercase().padStart(2, '0')

    fun rgbToHsl(rgb: Rgb): Hsl {
        val r = rgb.red / 255.0
        val g = rgb.green / 255.0
        val b = rgb.blue / 255.0
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min
        val l = (max + min) / 2.0
        val s = if (max == min) 0.0 else delta / (1.0 - abs(2.0 * l - 1.0))
        val h = when {
            delta == 0.0 -> 0.0
            max == r -> 60.0 * (((g - b) / delta) % 6.0).let { if (it < 0) it + 6.0 else it }
            max == g -> 60.0 * (((b - r) / delta) + 2.0)
            else -> 60.0 * (((r - g) / delta) + 4.0)
        }
        return Hsl(h, s * 100.0, l * 100.0)
    }

    /** WCAG-friendly suggestion: dark text on light backgrounds and vice versa (YIQ luminance). */
    fun readableTextColor(rgb: Rgb): String {
        val yiq = ((rgb.red * 299) + (rgb.green * 587) + (rgb.blue * 114)) / 1000.0
        return if (yiq >= 150) "#000000" else "#FFFFFF"
    }

    fun hslToRgb(h: Double, s: Double, l: Double): Rgb {
        val hn = ((h % 360.0) + 360.0) % 360.0 / 360.0
        val sn = (s.coerceIn(0.0, 100.0)) / 100.0
        val ln = (l.coerceIn(0.0, 100.0)) / 100.0
        if (sn == 0.0) {
            val v = (ln * 255.0).roundToInt()
            return Rgb(v, v, v)
        }
        val q = if (ln < 0.5) ln * (1 + sn) else ln + sn - ln * sn
        val p = 2 * ln - q
        fun channel(tIn: Double): Int {
            var t = tIn
            if (t < 0) t += 1.0
            if (t > 1) t -= 1.0
            val value = when {
                t < 1.0 / 6.0 -> p + (q - p) * 6 * t
                t < 1.0 / 2.0 -> q
                t < 2.0 / 3.0 -> p + (q - p) * (2.0 / 3.0 - t) * 6
                else -> p
            }
            return (value * 255.0).roundToInt()
        }
        return Rgb(channel(hn + 1.0 / 3.0), channel(hn), channel(hn - 1.0 / 3.0))
    }
}
