package com.drs.nexus.app.data

import android.content.Context
import android.net.Uri
import com.drs.nexus.core.util.CsvCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * CSV history export: streams every Room tool run into an RFC 4180 document
 * (UTF-8 with BOM for Excel/Arabic compatibility) via SAF CreateDocument —
 * no storage permission, user picks the destination.
 */
object CsvExporter {

    private val HEADER = listOf("tool_id", "executed_at_utc", "input", "output")

    fun buildCsv(rows: List<List<String>>): String = CsvCodec.BOM + CsvCodec.document(rows)

    fun runRows(runs: List<com.drs.nexus.core.data.ToolRunEntity>): List<List<String>> {
        val rows = mutableListOf(HEADER)
        runs.forEach { run ->
            rows.add(
                listOf(
                    run.toolId,
                    HistoryExporter.iso(run.executedAt),
                    run.inputPreview,
                    run.outputPreview
                )
            )
        }
        return rows
    }

    suspend fun writeTo(context: Context, uri: Uri, container: AppContainer): Result<Int> =
        withContext(Dispatchers.IO) {
            runCatching {
                val runs = container.toolRepository.allRunsOnce()
                val bytes = buildCsv(runRows(runs)).toByteArray(Charsets.UTF_8)
                context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                    out.write(bytes)
                    out.flush()
                } ?: throw IllegalStateException("Cannot open destination file")
                runs.size
            }
        }
}
