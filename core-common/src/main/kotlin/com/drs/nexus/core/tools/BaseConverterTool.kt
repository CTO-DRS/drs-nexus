package com.drs.nexus.core.tools

/**
 * Real number base converter supporting arbitrary bases 2–36.
 * Uses Kotlin stdlib `Int.toString(radix)` / `String.toInt(radix)` — no fake logic.
 */
class BaseConverterTool {

    data class ConvResult(
        val binary: String,
        val octal: String,
        val decimal: String,
        val hex: String,
        val customBase: String,
        val inputBase: Int,
        val inputValue: Long
    )

    sealed class ParseResult {
        data class Success(val value: Long, val inputBase: Int) : ParseResult()
        data class Failure(val message: String) : ParseResult()
    }

    companion object {
        const val DIGITS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val QUICK_BASES = listOf(2, 8, 10, 16)

        fun isValidInBase(text: String, base: Int): Boolean {
            if (base < 2 || base > 36) return false
            val t = text.trim().uppercase().removePrefix("-").removePrefix("+")
            if (t.isEmpty()) return false
            return t.all { ch ->
                val idx = DIGITS.indexOf(ch)
                idx in 0 until base
            }
        }
    }

    fun parse(text: String, base: Int): ParseResult {
        if (base < 2 || base > 36) return ParseResult.Failure("Base must be between 2 and 36")
        val t = text.trim()
        if (t.isEmpty()) return ParseResult.Failure("Input is empty")
        if (!isValidInBase(t, base)) {
            return ParseResult.Failure("'$t' is not a valid base-$base number")
        }
        return try {
            val value = t.toLong(base)
            ParseResult.Success(value, base)
        } catch (e: NumberFormatException) {
            ParseResult.Failure("Value out of 64-bit range for base $base")
        }
    }

    fun convert(value: Long, inputBase: Int, customBase: Int): ConvResult {
        val safeCustom = customBase.coerceIn(2, 36)
        return ConvResult(
            binary = value.toString(2),
            octal = value.toString(8),
            decimal = value.toString(10),
            hex = value.toString(16).uppercase(),
            customBase = value.toString(safeCustom).uppercase(),
            inputBase = inputBase,
            inputValue = value
        )
    }
}
