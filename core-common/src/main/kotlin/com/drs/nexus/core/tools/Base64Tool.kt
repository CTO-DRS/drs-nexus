package com.drs.nexus.core.tools

import java.util.Base64

/** Real Base64 encode/decode using java.util.Base64 (RFC 4648). */
class Base64Tool {

    sealed class Base64Result {
        data class Success(val text: String) : Base64Result()
        data class Failure(val message: String) : Base64Result()
    }

    fun encode(input: String, urlSafe: Boolean = false, withPadding: Boolean = true): String {
        val bytes = input.toByteArray(Charsets.UTF_8)
        val encoder = when {
            urlSafe && !withPadding -> Base64.getUrlEncoder().withoutPadding()
            urlSafe -> Base64.getUrlEncoder()
            !withPadding -> Base64.getEncoder().withoutPadding()
            else -> Base64.getEncoder()
        }
        return encoder.encodeToString(bytes)
    }

    fun decode(input: String, urlSafe: Boolean = false): Base64Result {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return Base64Result.Failure("Input is empty")
        val cleaned = trimmed.replace("\n", "").replace("\r", "").replace(" ", "")
        // Try the requested alphabet first, then the other RFC 4648 alphabet.
        // Deliberately NOT lenient: garbage like "!!!!" must fail, not decode to "".
        val decoders = if (urlSafe) listOf(Base64.getUrlDecoder(), Base64.getDecoder())
                       else listOf(Base64.getDecoder(), Base64.getUrlDecoder())
        for (decoder in decoders) {
            try {
                val bytes = decoder.decode(cleaned)
                val meaningfulInput = cleaned.any { it.isLetterOrDigit() || it in "+/=-_" }
                if (bytes.isEmpty() && meaningfulInput) continue // decoded to nothing from real chars → invalid
                return Base64Result.Success(String(bytes, Charsets.UTF_8))
            } catch (e: IllegalArgumentException) {
                // try next decoder
            }
        }
        return Base64Result.Failure("Invalid Base64: not a valid base64 alphabet or length")
    }
}
