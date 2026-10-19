package com.drs.nexus.core.util

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Pure (de)serialization codec for the full-backup feature.
 *
 * The backup document is intentionally decoupled from Android/Room/DataStore
 * types so it can be unit-tested on the JVM: settings travel as a plain
 * string map, favorites and runs as small data classes. Unknown fields are
 * tolerated so backups created by newer app versions still restore.
 */
object BackupCodec {

    const val SCHEMA_VERSION = 2
    const val APP_NAME = "DRS Nexus"

    @Serializable
    data class BackupFavorite(
        val toolId: String,
        val pinnedAtEpochMs: Long = 0
    )

    @Serializable
    data class BackupRun(
        val toolId: String,
        val input: String = "",
        val output: String = "",
        val executedAtEpochMs: Long = 0
    )

    @Serializable
    data class BackupDocument(
        val app: String = APP_NAME,
        val schemaVersion: Int = SCHEMA_VERSION,
        val appVersionName: String = "",
        val createdAtIso: String = "",
        val settings: Map<String, String> = emptyMap(),
        val favorites: List<BackupFavorite> = emptyList(),
        val runs: List<BackupRun> = emptyList()
    )

    val json: Json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(document: BackupDocument): String =
        json.encodeToString(BackupDocument.serializer(), document)

    /** Returns a parsed document or a failure with a human-readable reason. */
    fun decode(text: String): Result<BackupDocument> = runCatching {
        val doc = json.decodeFromString(BackupDocument.serializer(), text)
        require(doc.app == APP_NAME) { "Not a DRS Nexus backup file" }
        require(doc.schemaVersion in 1..SCHEMA_VERSION) {
            "Unsupported backup schema version ${doc.schemaVersion}"
        }
        doc
    }

    /**
     * Runs that would actually be imported: skips entries already present
     * (same toolId + executedAtEpochMs + input) among [existing] fingerprints.
     */
    fun deduplicateRuns(
        incoming: List<BackupRun>,
        existing: List<BackupRun>
    ): List<BackupRun> {
        val seen = existing.mapTo(HashSet()) { Triple(it.toolId, it.executedAtEpochMs, it.input) }
        val out = ArrayList<BackupRun>()
        for (run in incoming) {
            val key = Triple(run.toolId, run.executedAtEpochMs, run.input)
            if (seen.add(key)) out += run
        }
        return out
    }
}
