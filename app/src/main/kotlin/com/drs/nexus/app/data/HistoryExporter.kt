package com.drs.nexus.app.data

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.drs.nexus.app.BuildConfig
import com.drs.nexus.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Real usage-history export: serializes every Room tool run + favorite into a
 * versioned JSON document and writes it through the Storage Access Framework
 * (CreateDocument) — no storage permission required, user picks the destination.
 */
object HistoryExporter {

    @Serializable
    data class ExportRun(
        val toolId: String,
        val input: String,
        val output: String,
        val executedAtIso: String,
        val executedAtEpochMs: Long
    )

    @Serializable
    data class ExportFavorite(val toolId: String, val pinnedAtIso: String)

    @Serializable
    data class ExportDocument(
        val app: String = "DRS Nexus",
        val schemaVersion: Int = 1,
        val appVersionName: String,
        val exportedAtIso: String,
        val runCount: Int,
        val favoriteCount: Int,
        val runs: List<ExportRun>,
        val favorites: List<ExportFavorite>
    )

    private val json = Json { prettyPrint = true }

    fun iso(ms: Long): String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(Date(ms))

    suspend fun buildDocument(container: AppContainer): ExportDocument {
        val runs = container.toolRepository.allRunsOnce()
        val favs = container.toolRepository.favoritesOnce()
        return ExportDocument(
            appVersionName = BuildConfig.VERSION_NAME,
            exportedAtIso = iso(System.currentTimeMillis()),
            runCount = runs.size,
            favoriteCount = favs.size,
            runs = runs.map {
                ExportRun(it.toolId, it.inputPreview, it.outputPreview, iso(it.executedAt), it.executedAt)
            },
            favorites = favs.map { ExportFavorite(it.toolId, iso(it.pinnedAt)) }
        )
    }

    suspend fun writeTo(context: Context, uri: Uri, container: AppContainer): Result<Int> =
        withContext(Dispatchers.IO) {
            runCatching {
                val doc = buildDocument(container)
                val bytes = json.encodeToString(ExportDocument.serializer(), doc).toByteArray(Charsets.UTF_8)
                context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                    out.write(bytes)
                    out.flush()
                } ?: throw IllegalStateException("Cannot open destination file")
                doc.runCount
            }
        }
}

/** Settings row + SAF launcher for the export flow. */
@Composable
fun ExportHistoryRow(container: AppContainer) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val res = HistoryExporter.writeTo(context, uri, container)
                res.onSuccess { count ->
                    android.widget.Toast.makeText(
                        context,
                        context.getString(R.string.settings_export_done, count),
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }.onFailure {
                    android.widget.Toast.makeText(
                        context,
                        context.getString(R.string.settings_export_failed, it.message ?: ""),
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.FileDownload,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.settings_export_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                stringResource(R.string.settings_export_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        com.drs.nexus.app.ui.components.ActionButton(
            icon = Icons.Filled.FileDownload,
            label = stringResource(R.string.settings_export_button),
            onClick = { launcher.launch("drs-nexus-history.json") }
        )
    }
}
