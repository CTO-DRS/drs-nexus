package com.drs.nexus.app.data

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.SettingsBackupRestore
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
import com.drs.nexus.core.data.ToolRunEntity
import com.drs.nexus.core.util.BackupCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Full backup: settings + favorites + history in one versioned JSON document.
 *
 * - Create: SAF CreateDocument -> user picks destination (no storage permission).
 * - Restore: SAF OpenDocument -> validate -> merge (settings applied, favorites
 *   unioned by toolId, history deduplicated) so repeated restores are safe.
 */
object BackupManager {

    @Serializable
    data class BackupStats(
        val runs: Int,
        val favorites: Int,
        val settingsApplied: Int
    )

    private val json = BackupCodec.json

    fun iso(ms: Long): String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(Date(ms))

    // ---------------- Create ----------------

    suspend fun writeTo(context: Context, uri: Uri, container: AppContainer): Result<BackupStats> =
        withContext(Dispatchers.IO) {
            runCatching {
                val settingsMap = container.settingsRepository.snapshot()
                val favs = container.toolRepository.favoritesOnce()
                val runs = container.toolRepository.allRunsOnce()

                val document = BackupCodec.BackupDocument(
                    appVersionName = BuildConfig.VERSION_NAME,
                    createdAtIso = iso(System.currentTimeMillis()),
                    settings = settingsMap,
                    favorites = favs.map {
                        BackupCodec.BackupFavorite(it.toolId, it.pinnedAt)
                    },
                    runs = runs.map {
                        BackupCodec.BackupRun(it.toolId, it.inputPreview, it.outputPreview, it.executedAt)
                    }
                )

                val bytes = BackupCodec.encode(document).toByteArray(Charsets.UTF_8)
                context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                    out.write(bytes)
                    out.flush()
                } ?: throw IllegalStateException("Cannot open destination file")

                BackupStats(
                    runs = runs.size,
                    favorites = favs.size,
                    settingsApplied = settingsMap.size
                )
            }
        }

    // ---------------- Restore ----------------

    suspend fun restoreFrom(context: Context, uri: Uri, container: AppContainer): Result<BackupStats> =
        withContext(Dispatchers.IO) {
            runCatching {
                val text = context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.readBytes().toString(Charsets.UTF_8)
                } ?: throw IllegalStateException("Cannot open backup file")

                val doc = BackupCodec.decode(text).getOrElse { throw it }

                // 1) Settings (validated value-by-value inside the repository)
                val settingsApplied = container.settingsRepository.restore(doc.settings)

                // 2) Favorites — union by toolId, keep earliest pinnedAt
                val existingFavs = container.toolRepository.favoritesOnce()
                val existingFavIds = existingFavs.associateByTo(HashMap()) { it.toolId }
                var favsAdded = 0
                for (fav in doc.favorites) {
                    if (fav.toolId.isBlank()) continue
                    val current = existingFavIds[fav.toolId]
                    if (current == null) {
                        container.toolRepository.addFavoritePinned(fav.toolId, fav.pinnedAtEpochMs)
                        favsAdded++
                    } else if (fav.pinnedAtEpochMs in 1 until current.pinnedAt) {
                        // keep the older (original) pin date for fidelity
                        container.toolRepository.addFavoritePinned(fav.toolId, fav.pinnedAtEpochMs)
                    }
                }

                // 3) History — deduplicated against what is already stored
                val existingRuns = container.toolRepository.allRunsOnce()
                val existingFingerprints = existingRuns.map {
                    BackupCodec.BackupRun(it.toolId, it.inputPreview, it.outputPreview, it.executedAt)
                }
                val toImport = BackupCodec.deduplicateRuns(doc.runs, existingFingerprints)
                for (run in toImport) {
                    container.toolRepository.importRun(
                        ToolRunEntity(
                            toolId = run.toolId,
                            inputPreview = run.input,
                            outputPreview = run.output,
                            executedAt = run.executedAtEpochMs
                        )
                    )
                }

                BackupStats(
                    runs = toImport.size,
                    favorites = favsAdded,
                    settingsApplied = settingsApplied
                )
            }
        }
}

// ---------------- Settings UI rows ----------------

private fun toast(context: Context, message: String) {
    android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
}

/** "Create full backup" row: settings + favorites + history -> one JSON file via SAF. */
@androidx.compose.runtime.Composable
fun BackupCreateRow(container: AppContainer) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                BackupManager.writeTo(context, uri, container)
                    .onSuccess { stats ->
                        toast(
                            context,
                            context.getString(
                                R.string.backup_done,
                                stats.runs,
                                stats.favorites,
                                stats.settingsApplied
                            )
                        )
                    }
                    .onFailure {
                        toast(context, context.getString(R.string.backup_failed, it.message ?: ""))
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
            Icons.Filled.CloudSync,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.backup_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                stringResource(R.string.backup_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        com.drs.nexus.app.ui.components.ActionButton(
            icon = Icons.Filled.CloudSync,
            label = stringResource(R.string.backup_button),
            onClick = { launcher.launch("drs-nexus-backup.json") }
        )
    }
}

/** "Restore backup" row: SAF OpenDocument -> validated merge back into the app. */
@androidx.compose.runtime.Composable
fun BackupRestoreRow(container: AppContainer) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                BackupManager.restoreFrom(context, uri, container)
                    .onSuccess { stats ->
                        toast(
                            context,
                            context.getString(
                                R.string.backup_restore_done,
                                stats.runs,
                                stats.favorites,
                                stats.settingsApplied
                            )
                        )
                    }
                    .onFailure {
                        toast(context, context.getString(R.string.restore_failed, it.message ?: ""))
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
            Icons.Filled.SettingsBackupRestore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.backup_restore_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                stringResource(R.string.backup_restore_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        com.drs.nexus.app.ui.components.ActionButton(
            icon = Icons.Filled.SettingsBackupRestore,
            label = stringResource(R.string.backup_restore_button),
            onClick = { launcher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }
        )
    }
}
