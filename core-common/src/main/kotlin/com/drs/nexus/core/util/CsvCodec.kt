package com.drs.nexus.core.util

/**
 * Minimal RFC 4180-compliant CSV codec: quoting, quote doubling and CRLF
 * row separation. Pure logic — unit-tested in core-common, consumed by the
 * app-side CSV history exporter.
 */
object CsvCodec {

    /** UTF-8 BOM so Excel/most viewers detect UTF-8 (Arabic renders correctly). */
    const val BOM: String = "\uFEFF"

    /** Quotes a field only when required: comma, quote, CR or LF present. */
    fun escape(field: String): String {
        val needsQuoting = field.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        return if (needsQuoting) "\"" + field.replace("\"", "\"\"") + "\"" else field
    }

    /** One CSV line from fields. */
    fun line(vararg fields: String): String = fields.joinToString(",") { escape(it) }

    /**
     * Full document: header row first, CRLF separators (RFC 4180), trailing
     * CRLF so the file ends with a complete line.
     */
    fun document(rows: List<List<String>>): String =
        rows.joinToString("\r\n") { row -> row.joinToString(",") { escape(it) } } + "\r\n"
}
