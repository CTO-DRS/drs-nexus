package com.drs.nexus.core.tools

import java.security.SecureRandom
import java.util.UUID

/** Real UUID v4 generator with formatting options. */
class UuidTool {

    data class UuidOptions(val uppercase: Boolean = false, val withHyphens: Boolean = true, val braces: Boolean = false)

    fun generate(count: Int = 1, options: UuidOptions = UuidOptions()): List<String> {
        val n = count.coerceIn(1, 100)
        return (1..n).map {
            var v = UUID.randomUUID().toString()
            if (!options.withHyphens) v = v.replace("-", "")
            if (options.uppercase) v = v.uppercase()
            if (options.braces) v = "{$v}"
            v
        }
    }

    /** Returns version nibble, or null if invalid. */
    fun version(uuid: String): Int? {
        val clean = uuid.trim().replace("-", "").replace("{", "").replace("}", "")
        if (clean.length != 32) return null
        val v = clean[12].digitToIntOrNull() ?: return null
        return if (v in 1..8 || v == 4) v else null
    }
}

/** Real cryptographically-secure password generator. */
class PasswordGenerator {

    data class Options(
        val length: Int = 16,
        val uppercase: Boolean = true,
        val lowercase: Boolean = true,
        val digits: Boolean = true,
        val symbols: Boolean = true,
        val excludeAmbiguous: Boolean = false
    )

    enum class Strength(val label: String) { WEAK("Weak"), FAIR("Fair"), STRONG("Strong"), VERY_STRONG("Very Strong") }

    private val upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private val lower = "abcdefghijklmnopqrstuvwxyz"
    private val digits = "0123456789"
    private val symbols = "!@#\$%^&*()-_=+[]{};:,.<>?"
    private val ambiguous = "Il1O0"
    private val rng = SecureRandom()

    fun generate(options: Options): String {
        val length = options.length.coerceIn(4, 128)
        fun usable(base: String): String =
            if (options.excludeAmbiguous) base.filter { it !in ambiguous } else base

        val pools = listOfNotNull(
            if (options.uppercase) usable(upper).takeIf { it.isNotEmpty() } else null,
            if (options.lowercase) usable(lower).takeIf { it.isNotEmpty() } else null,
            if (options.digits) usable(digits).takeIf { it.isNotEmpty() } else null,
            if (options.symbols) usable(symbols).takeIf { it.isNotEmpty() } else null
        )
        require(pools.isNotEmpty()) { "At least one character class must be selected" }
        val pool = pools.joinToString("")

        // Guarantee at least one char from every selected class, then fill randomly.
        val chars = mutableListOf<Char>()
        pools.forEach { chars.add(pick(it)) }
        repeat((length - pools.size).coerceAtLeast(0)) { chars.add(pick(pool)) }
        while (chars.size > length) chars.removeAt(chars.size - 1)
        chars.shuffle(rng)
        return chars.joinToString("")
    }

    /** Entropy-based strength estimation in bits. */
    fun entropyBits(options: Options): Int {
        var poolSize = 0
        if (options.uppercase) poolSize += 26
        if (options.lowercase) poolSize += 26
        if (options.digits) poolSize += 10
        if (options.symbols) poolSize += symbols.length
        if (options.excludeAmbiguous) poolSize -= 5.coerceAtMost(poolSize)
        if (poolSize <= 0) return 0
        return (options.length * (Math.log(poolSize.toDouble()) / Math.log(2.0))).toInt()
    }

    fun strength(bits: Int): Strength = when {
        bits >= 100 -> Strength.VERY_STRONG
        bits >= 70 -> Strength.STRONG
        bits >= 45 -> Strength.FAIR
        else -> Strength.WEAK
    }

    private fun pick(s: String): Char = s[rng.nextInt(s.length)]
}
