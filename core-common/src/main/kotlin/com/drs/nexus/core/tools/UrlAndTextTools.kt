package com.drs.nexus.core.tools

import java.net.URLDecoder
import java.net.URLEncoder
import java.text.Normalizer

/** Real URL encode/decode + case conversion utilities. */
class UrlTool {

    sealed class UrlResult {
        data class Success(val text: String) : UrlResult()
        data class Failure(val message: String) : UrlResult()
    }

    fun encode(input: String, plusForSpace: Boolean = true): String {
        val encoded = URLEncoder.encode(input, Charsets.UTF_8)
        return if (plusForSpace) encoded else encoded.replace("+", "%20")
    }

    fun decode(input: String): UrlResult {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return UrlResult.Failure("Input is empty")
        return try {
            UrlResult.Success(URLDecoder.decode(trimmed, Charsets.UTF_8))
        } catch (e: IllegalArgumentException) {
            UrlResult.Failure("Invalid percent-encoding sequence")
        }
    }
}

/** Real text utilities: case conversion, counting, reverse, normalize. */
class TextUtilitiesTool {

    enum class CaseStyle { CAMEL, SNAKE, KEBAB, PASCAL, UPPER, LOWER, TITLE }

    data class TextStats(
        val characters: Int,
        val charactersNoSpaces: Int,
        val words: Int,
        val lines: Int,
        val sentences: Int,
        val paragraphs: Int,
        val uniqueWords: Int
    )

    fun stats(text: String): TextStats {
        val words = tokenize(text)
        return TextStats(
            characters = text.length,
            charactersNoSpaces = text.count { !it.isWhitespace() },
            words = words.size,
            lines = if (text.isEmpty()) 0 else text.lines().size,
            sentences = text.split(Regex("[.!?؟]+\\s+|[.!?؟]+$")).count { it.isNotBlank() },
            paragraphs = text.split(Regex("\\n\\s*\\n")).count { it.isNotBlank() },
            uniqueWords = words.map { it.lowercase() }.toSet().size
        )
    }

    fun convertCase(text: String, style: CaseStyle): String {
        val words = tokenize(text)
        return when (style) {
            CaseStyle.CAMEL -> words.mapIndexed { i, w ->
                if (i == 0) w.lowercase() else w.lowercase().replaceFirstChar { it.uppercase() }
            }.joinToString("")
            CaseStyle.PASCAL -> words.joinToString("") { it.lowercase().replaceFirstChar { c -> c.uppercase() } }
            CaseStyle.SNAKE -> words.joinToString("_") { it.lowercase() }
            CaseStyle.KEBAB -> words.joinToString("-") { it.lowercase() }
            CaseStyle.UPPER -> text.uppercase()
            CaseStyle.LOWER -> text.lowercase()
            CaseStyle.TITLE -> words.joinToString(" ") { it.lowercase().replaceFirstChar { c -> c.uppercase() } }
        }
    }

    fun reverse(text: String): String = text.reversed()

    fun trimLines(text: String, removeEmpty: Boolean = false): String =
        text.lines().map { it.trim() }.filter { !removeEmpty || it.isNotEmpty() }.joinToString("\n")

    fun removeExtraSpaces(text: String): String = text.replace(Regex("\\s+"), " ").trim()

    fun normalizeUnicode(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFC)
            .replace(Regex("[\\u200F\\u200E\\u202A-\\u202E\\uFEFF]"), "")

    private fun tokenize(text: String): List<String> =
        Regex("[\\p{L}\\p{N}]+").findAll(text).map { it.value }.toList()
}
