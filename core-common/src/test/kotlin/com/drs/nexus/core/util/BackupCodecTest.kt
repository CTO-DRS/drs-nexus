package com.drs.nexus.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCodecTest {

    private val doc = BackupCodec.BackupDocument(
        appVersionName = "5.1.0",
        createdAtIso = "2026-10-18T20:00:00Z",
        settings = mapOf(
            "theme_mode" to "SCHEDULED",
            "app_language" to "ARABIC",
            "haptics_enabled" to "true",
            "dark_from" to "20:30",
            "dark_to" to "06:15"
        ),
        favorites = listOf(
            BackupCodec.BackupFavorite("json", 1760000000000),
            BackupCodec.BackupFavorite("hash", 1760000100000)
        ),
        runs = listOf(
            BackupCodec.BackupRun("json", "{\"a\":1}", "formatted", 1760000000000),
            BackupCodec.BackupRun("hash", "hello", "sha256:...", 1760000300000)
        )
    )

    @Test
    fun `encode then decode round-trips the full document`() {
        val text = BackupCodec.encode(doc)
        val restored = BackupCodec.decode(text).getOrThrow()
        assertEquals(doc, restored)
    }

    @Test
    fun `encoded document is pretty-printed JSON with schema markers`() {
        val text = BackupCodec.encode(doc)
        assertTrue(text.contains("\"schemaVersion\": 2"))
        assertTrue(text.contains("\"app\": \"DRS Nexus\""))
        assertTrue(text.contains("\"dark_from\""))
    }

    @Test
    fun `decode tolerates unknown fields from newer versions`() {
        val text = """
            {
              "app": "DRS Nexus",
              "schemaVersion": 2,
              "futureField": {"nested": true},
              "settings": {"theme_mode": "DARK"},
              "favorites": [],
              "runs": []
            }
        """.trimIndent()
        val restored = BackupCodec.decode(text).getOrThrow()
        assertEquals("DARK", restored.settings["theme_mode"])
        assertTrue(restored.favorites.isEmpty())
    }

    @Test
    fun `decode applies defaults for missing optional fields`() {
        val text = """
            {"app": "DRS Nexus", "schemaVersion": 1}
        """.trimIndent()
        val restored = BackupCodec.decode(text).getOrThrow()
        assertEquals(1, restored.schemaVersion)
        assertEquals(emptyMap<String, String>(), restored.settings)
        assertEquals(emptyList<BackupCodec.BackupRun>(), restored.runs)
        assertEquals(emptyList<BackupCodec.BackupFavorite>(), restored.favorites)
    }

    @Test
    fun `decode rejects non-DRS documents`() {
        val res = BackupCodec.decode("""{"app": "Other", "schemaVersion": 1}""")
        assertTrue(res.isFailure)
    }

    @Test
    fun `decode rejects unsupported future schema`() {
        val res = BackupCodec.decode("""{"app": "DRS Nexus", "schemaVersion": 99}""")
        assertTrue(res.isFailure)
    }

    @Test
    fun `decode rejects corrupt json with readable failure`() {
        val res = BackupCodec.decode("{not json at all")
        assertTrue(res.isFailure)
    }

    @Test
    fun `decode rejects empty input`() {
        val res = BackupCodec.decode("")
        assertTrue(res.isFailure)
    }

    @Test
    fun `deduplicateRuns skips already-present entries`() {
        val existing = listOf(BackupCodec.BackupRun("json", "{\"a\":1}", "formatted", 1760000000000))
        val incoming = listOf(
            BackupCodec.BackupRun("json", "{\"a\":1}", "formatted", 1760000000000), // duplicate
            BackupCodec.BackupRun("hash", "hello", "sha256:...", 1760000300000)     // new
        )
        val result = BackupCodec.deduplicateRuns(incoming, existing)
        assertEquals(1, result.size)
        assertEquals("hash", result.first().toolId)
    }

    @Test
    fun `deduplicateRuns keeps both identical-input runs at different times`() {
        val run = BackupCodec.BackupRun("uuid", "", "abc", 1000)
        val result = BackupCodec.deduplicateRuns(listOf(run, run.copy(executedAtEpochMs = 2000)), listOf(run))
        assertEquals(1, result.size)
        assertEquals(2000, result.first().executedAtEpochMs)
    }
}
