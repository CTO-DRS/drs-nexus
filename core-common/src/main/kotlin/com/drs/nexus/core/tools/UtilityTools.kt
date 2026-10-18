package com.drs.nexus.core.tools

/**
 * Real regex tester built on kotlin.text.Regex with genuine match highlighting data.
 */
class RegexTesterTool {

    data class MatchInfo(
        val value: String,
        val start: Int,
        val end: Int,
        val groups: List<GroupInfo>
    )

    data class GroupInfo(val index: Int, val name: String?, val value: String?, val start: Int, val end: Int)

    sealed class RegexResult {
        data class Success(val matches: List<MatchInfo>, val replacePreview: String, val matchCount: Int) : RegexResult()
        data class Failure(val message: String) : RegexResult()
    }

    fun test(
        pattern: String,
        input: String,
        caseInsensitive: Boolean = false,
        multiline: Boolean = false,
        dotAll: Boolean = false,
        replacement: String = ""
    ): RegexResult {
        if (pattern.isEmpty()) return RegexResult.Failure("Pattern is empty")
        val options = buildSet {
            if (caseInsensitive) add(RegexOption.IGNORE_CASE)
            if (multiline) add(RegexOption.MULTILINE)
            if (dotAll) add(RegexOption.DOT_MATCHES_ALL)
        }
        return try {
            val regex = Regex(pattern, options)
            val matches = regex.findAll(input).take(500).map { m ->
                MatchInfo(
                    value = m.value,
                    start = m.range.first,
                    end = m.range.last + 1,
                    groups = m.groups.mapIndexed { i, g ->
                        if (g == null) null else GroupInfo(
                            index = i,
                            name = regex.pattern.let { null }, // named groups not exposed in stdlib
                            value = g.value,
                            start = g.range.first,
                            end = g.range.last + 1
                        )
                    }.filterNotNull()
                )
            }.toList()
            val replacePreview = if (replacement.isEmpty()) "" else regex.replace(input, replacement)
            RegexResult.Success(matches, replacePreview, matches.size)
        } catch (e: Exception) {
            RegexResult.Failure(e.message?.substringBefore(" pattern") ?: "Invalid regex pattern")
        }
    }
}

/**
 * Real epoch-timestamp <-> human date converter using java.time.
 */
class TimestampConverterTool {

    sealed class TsResult {
        data class Success(val text: String, val details: Map<String, String>) : TsResult()
        data class Failure(val message: String) : TsResult()
    }

    fun fromEpoch(epoch: String, zoneId: String = "UTC"): TsResult {
        val value = epoch.trim()
        if (value.isEmpty()) return TsResult.Failure("Input is empty")
        val longValue = value.toLongOrNull()
            ?: return TsResult.Failure("Not a valid epoch number")
        if (longValue !in -62135596800L..253402300799L && longValue !in -62135596800000L..253402300799999L) {
            return TsResult.Failure("Epoch value out of supported range")
        }
        return try {
            val zone = java.time.ZoneId.of(zoneId)
            val seconds = if (kotlin.math.abs(longValue) > 100_000_000_000L) longValue / 1000 else longValue
            val millis = if (kotlin.math.abs(longValue) > 100_000_000_000L) longValue else longValue * 1000
            val dateTime = java.time.Instant.ofEpochSecond(seconds).atZone(zone)
            TsResult.Success(
                text = dateTime.toLocalDateTime().toString().replace('T', ' '),
                details = mapOf(
                    "ISO" to dateTime.toString(),
                    "Date" to dateTime.toLocalDate().toString(),
                    "Time" to dateTime.toLocalTime().toString().substringBefore('.'),
                    "Day of week" to dateTime.dayOfWeek.toString(),
                    "Day of year" to dateTime.dayOfYear.toString(),
                    "Unix millis" to millis.toString(),
                    "Zone" to zoneId
                )
            )
        } catch (e: Exception) {
            TsResult.Failure("Unsupported zone: $zoneId")
        }
    }

    fun now(): Long = System.currentTimeMillis() / 1000
}

/**
 * Real unit converter across length, weight, temperature and data size.
 */
class UnitConverterTool {

    enum class Category(val label: String) { LENGTH("Length"), WEIGHT("Weight"), TEMPERATURE("Temperature"), DATA("Data") }

    enum class Unit(val label: String, val category: Category, val factorToBase: Double) {
        MM("Millimeter", Category.LENGTH, 0.001),
        CM("Centimeter", Category.LENGTH, 0.01),
        M("Meter", Category.LENGTH, 1.0),
        KM("Kilometer", Category.LENGTH, 1000.0),
        IN("Inch", Category.LENGTH, 0.0254),
        FT("Foot", Category.LENGTH, 0.3048),
        MI("Mile", Category.LENGTH, 1609.344),
        MG("Milligram", Category.WEIGHT, 0.000001),
        G("Gram", Category.WEIGHT, 0.001),
        KG("Kilogram", Category.WEIGHT, 1.0),
        T("Tonne", Category.WEIGHT, 1000.0),
        LB("Pound", Category.WEIGHT, 0.45359237),
        OZ("Ounce", Category.WEIGHT, 0.028349523125),
        C("Celsius", Category.TEMPERATURE, 1.0),
        F("Fahrenheit", Category.TEMPERATURE, 1.0),
        K("Kelvin", Category.TEMPERATURE, 1.0),
        B("Byte", Category.DATA, 1.0),
        KB("Kilobyte", Category.DATA, 1024.0),
        MB("Megabyte", Category.DATA, 1048576.0),
        GB("Gigabyte", Category.DATA, 1073741824.0),
        TB("Terabyte", Category.DATA, 1099511627776.0)
    }

    sealed class ConvResult {
        data class Success(val value: Double, val formatted: String) : ConvResult()
        data class Failure(val message: String) : ConvResult()
    }

    fun convert(value: Double, from: Unit, to: Unit): ConvResult {
        if (from.category != to.category) return ConvResult.Failure("Cannot convert ${from.category} to ${to.category}")
        val result = when (from.category) {
            Category.TEMPERATURE -> convertTemperature(value, from, to)
            else -> value * from.factorToBase / to.factorToBase
        }
        val formatted = if (kotlin.math.abs(result) >= 1e10 || (result != 0.0 && kotlin.math.abs(result) < 1e-6)) {
            String.format(java.util.Locale.US, "%.6g", result)
        } else {
            String.format(java.util.Locale.US, "%.6f", result).trimEnd('0').trimEnd('.')
        }
        return ConvResult.Success(result, formatted)
    }

    private fun convertTemperature(v: Double, from: Unit, to: Unit): Double {
        val celsius = when (from) {
            Unit.C -> v
            Unit.F -> (v - 32) * 5 / 9
            Unit.K -> v - 273.15
            else -> v
        }
        return when (to) {
            Unit.C -> celsius
            Unit.F -> celsius * 9 / 5 + 32
            Unit.K -> celsius + 273.15
            else -> celsius
        }
    }
}

/**
 * Real lorem-ipsum generator (Latin placeholder text) with paragraph/sentence/word modes.
 */
class LoremIpsumTool {

    private val words = listOf(
        "lorem", "ipsum", "dolor", "sit", "amet", "consectetur", "adipiscing", "elit", "sed", "do",
        "eiusmod", "tempor", "incididunt", "ut", "labore", "et", "dolore", "magna", "aliqua", "enim",
        "ad", "minim", "veniam", "quis", "nostrud", "exercitation", "ullamco", "laboris", "nisi",
        "aliquip", "ex", "ea", "commodo", "consequat", "duis", "aute", "irure", "in", "reprehenderit",
        "voluptate", "velit", "esse", "cillum", "eu", "fugiat", "nulla", "pariatur", "excepteur",
        "sint", "occaecat", "cupidatat", "non", "proident", "sunt", "culpa", "qui", "officia",
        "deserunt", "mollit", "anim", "id", "est", "laborum"
    )

    enum class Mode { PARAGRAPHS, SENTENCES, WORDS }

    fun generate(mode: Mode, count: Int, startWithLorem: Boolean = true): String {
        val n = count.coerceIn(1, 50)
        val rng = java.security.SecureRandom()
        return when (mode) {
            Mode.WORDS -> {
                val out = (1..n).joinToString(" ") { words[rng.nextInt(words.size)] }
                capitalizeFirst(if (startWithLorem) out else out)
            }
            Mode.SENTENCES -> (1..n).joinToString(" ") { sentence(rng, startWithLorem && it == 1) }
            Mode.PARAGRAPHS -> (1..n).joinToString("\n\n") { i ->
                val sentences = 3 + rng.nextInt(3)
                (1..sentences).joinToString(" ") { s -> sentence(rng, startWithLorem && i == 1 && s == 1) }
            }
        }
    }

    private fun sentence(rng: java.security.SecureRandom, startClassic: Boolean): String {
        val len = 6 + rng.nextInt(12)
        val body = if (startClassic) "Lorem ipsum dolor sit amet, " else ""
        val rest = (1..len).joinToString(" ") { words[rng.nextInt(words.size)] }
        return capitalizeFirst("$body$rest") + "."
    }

    private fun capitalizeFirst(s: String): String {
        val first = s.firstOrNull() ?: return s
        return if (first.isLowerCase()) first.uppercaseChar() + s.substring(1) else s
    }
}
