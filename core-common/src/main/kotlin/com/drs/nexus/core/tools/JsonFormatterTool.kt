package com.drs.nexus.core.tools

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

/**
 * Real JSON parser / formatter / validator built on kotlinx.serialization.
 * No fake output: every operation parses the input and reports genuine errors
 * with line/column position when parsing fails.
 */
class JsonFormatterTool(private val indent: Int = 2) {

    sealed class JsonResult {
        data class Success(val formatted: String, val minified: String, val stats: Stats) : JsonResult()
        data class Failure(val message: String, val line: Int, val column: Int) : JsonResult()
    }

    data class Stats(
        val keys: Int,
        val values: Int,
        val maxDepth: Int,
        val objects: Int,
        val arrays: Int,
        val sizeBytes: Int
    )

    private val json = Json { ignoreUnknownKeys = false; isLenient = false }

    fun format(input: String): JsonResult {
        if (input.isBlank()) return JsonResult.Failure("Input is empty", 1, 1)
        return try {
            val element = json.parseToJsonElement(input)
            val formatted = render(element, indent, 0)
            val minified = render(element, 0, 0)
            JsonResult.Success(
                formatted = formatted,
                minified = minified,
                stats = computeStats(element, minified.length)
            )
        } catch (e: Exception) {
            val pos = extractPosition(e, input)
            JsonResult.Failure(cleanMessage(e.message ?: "Invalid JSON"), pos.first, pos.second)
        }
    }

    fun validate(input: String): JsonResult = format(input)

    fun minify(input: String): String? {
        return try {
            render(json.parseToJsonElement(input), 0, 0)
        } catch (e: Exception) {
            null
        }
    }

    private fun cleanMessage(raw: String): String = raw
        .replace("Unexpected JSON token at offset", "Invalid JSON at offset")
        .substringBefore(". JSON input:")
        .trim()

    private fun extractPosition(e: Exception, input: String): Pair<Int, Int> {
        val offsetRegex = Regex("at offset (\\d+)").find(e.message ?: "") ?: return 1 to 1
        val offset = offsetRegex.groupValues[1].toIntOrNull() ?: return 1 to 1
        val before = input.substring(0, offset.coerceAtMost(input.length))
        val line = before.count { it == '\n' } + 1
        val column = before.length - before.lastIndexOf('\n')
        return line to column
    }

    private fun render(element: JsonElement, indentSize: Int, level: Int): String {
        val pad = if (indentSize <= 0) "" else " ".repeat(indentSize * (level + 1))
        val padClose = if (indentSize <= 0) "" else " ".repeat(indentSize * level)
        return when (element) {
            is JsonObject -> {
                if (element.isEmpty()) return "{}"
                val body = element.entries.joinToString(if (indentSize <= 0) "," else ",\n") { (key, value) ->
                    val keyPart = JsonPrimitive(key).toString()
                    if (indentSize <= 0) "$keyPart:${render(value, 0, level)}"
                    else "$pad$keyPart: ${render(value, indentSize, level + 1)}"
                }
                if (indentSize <= 0) "{$body}" else "{\n$body\n$padClose}"
            }
            is kotlinx.serialization.json.JsonArray -> {
                if (element.isEmpty()) return "[]"
                val body = element.joinToString(if (indentSize <= 0) "," else ",\n") { v ->
                    if (indentSize <= 0) render(v, 0, level) else pad + render(v, indentSize, level + 1)
                }
                if (indentSize <= 0) "[$body]" else "[\n$body\n$padClose]"
            }
            is JsonPrimitive -> element.toString()
        }
    }

    private fun computeStats(element: JsonElement, minifiedSize: Int): Stats {
        var keys = 0; var values = 0; var objects = 0; var arrays = 0; var maxDepth = 0
        fun walk(el: JsonElement, depth: Int) {
            maxDepth = maxOf(maxDepth, depth)
            when (el) {
                is JsonObject -> {
                    objects++; keys += el.size
                    el.values.forEach { walk(it, depth + 1) }
                }
                is kotlinx.serialization.json.JsonArray -> {
                    arrays++
                    el.forEach { walk(it, depth + 1) }
                }
                else -> values++
            }
        }
        walk(element, 1)
        return Stats(keys, values, maxDepth, objects, arrays, minifiedSize)
    }
}
