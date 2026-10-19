package com.drs.nexus.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "nexus_settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK, SCHEDULED }
enum class AppLanguage { SYSTEM, ARABIC, ENGLISH }

data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val onboardingDone: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val darkFrom: String = "19:00",
    val darkTo: String = "06:00"
)

/** Real DataStore-backed settings repository. */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val LANGUAGE = stringPreferencesKey("app_language")
        val ONBOARDING = booleanPreferencesKey("onboarding_done")
        val HAPTICS = booleanPreferencesKey("haptics_enabled")
        val DARK_FROM = stringPreferencesKey("dark_from")
        val DARK_TO = stringPreferencesKey("dark_to")
    }

    val settings: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            themeMode = prefs[Keys.THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
            language = prefs[Keys.LANGUAGE]?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() } ?: AppLanguage.SYSTEM,
            onboardingDone = prefs[Keys.ONBOARDING] ?: false,
            hapticsEnabled = prefs[Keys.HAPTICS] ?: true,
            darkFrom = prefs[Keys.DARK_FROM] ?: "19:00",
            darkTo = prefs[Keys.DARK_TO] ?: "06:00"
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME] = mode.name }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { it[Keys.LANGUAGE] = language.name }
    }

    suspend fun setOnboardingDone(done: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING] = done }
    }

    suspend fun setHaptics(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HAPTICS] = enabled }
    }

    /** Stores the scheduled-dark window as two canonical "HH:mm" strings. */
    suspend fun setScheduledWindow(fromHm: String, toHm: String) {
        context.dataStore.edit {
            it[Keys.DARK_FROM] = fromHm
            it[Keys.DARK_TO] = toHm
        }
    }

    /**
     * Exports every setting as a plain string map — the settings payload of
     * the full-backup document.
     */
    suspend fun snapshot(): Map<String, String> {
        val s = settings.first()
        return buildMap {
            put("theme_mode", s.themeMode.name)
            put("app_language", s.language.name)
            put("onboarding_done", s.onboardingDone.toString())
            put("haptics_enabled", s.hapticsEnabled.toString())
            put("dark_from", s.darkFrom)
            put("dark_to", s.darkTo)
        }
    }

    /**
     * Restores settings from a backup map. Unknown keys are ignored and every
     * value is validated before being applied, so a partially corrupt backup
     * can never put the app into a broken state.
     */
    suspend fun restore(values: Map<String, String>): Int {
        var applied = 0
        suspend fun <T> apply(key: String, valid: (String) -> T?, commit: suspend (T) -> Unit) {
            val raw = values[key] ?: return
            val parsed = valid(raw) ?: return
            commit(parsed)
            applied++
        }
        apply("theme_mode", { runCatching { ThemeMode.valueOf(it) }.getOrNull() }) { setThemeMode(it) }
        apply("app_language", { runCatching { AppLanguage.valueOf(it) }.getOrNull() }) { setLanguage(it) }
        apply("onboarding_done", { it.toBooleanStrictOrNull() }) { setOnboardingDone(it) }
        apply("haptics_enabled", { it.toBooleanStrictOrNull() }) { setHaptics(it) }
        val from = values["dark_from"]?.let { com.drs.nexus.core.util.ThemeSchedule.parseHm(it) }
        val to = values["dark_to"]?.let { com.drs.nexus.core.util.ThemeSchedule.parseHm(it) }
        if (from != null && to != null) {
            setScheduledWindow(
                com.drs.nexus.core.util.ThemeSchedule.formatHm(from),
                com.drs.nexus.core.util.ThemeSchedule.formatHm(to)
            )
            applied++
        }
        return applied
    }
}

/** Repository wiring history + favorites. */
class ToolRepository(
    private val runDao: ToolRunDao,
    private val favoriteDao: FavoriteDao
) {
    fun recentRuns(limit: Int = 100): Flow<List<ToolRunEntity>> = runDao.recentRuns(limit)
    fun runsForTool(toolId: String): Flow<List<ToolRunEntity>> = runDao.runsForTool(toolId)
    suspend fun logRun(toolId: String, input: String, output: String) {
        runDao.insert(
            ToolRunEntity(
                toolId = toolId,
                inputPreview = input.take(200),
                outputPreview = output.take(200)
            )
        )
    }
    suspend fun deleteRun(id: Long) = runDao.deleteById(id)
    suspend fun clearHistory() = runDao.clearAll()
    suspend fun allRunsOnce() = runDao.allRunsOnce()
    suspend fun favoritesOnce() = favoriteDao.allOnce()

    /** Inserts a history run verbatim (used by backup restore). */
    suspend fun importRun(run: ToolRunEntity) {
        runDao.insert(run)
    }

    /** Adds/updates a favorite while preserving its original pin date. */
    suspend fun addFavoritePinned(toolId: String, pinnedAt: Long) {
        if (pinnedAt > 0) favoriteDao.add(FavoriteEntity(toolId, pinnedAt))
        else favoriteDao.add(FavoriteEntity(toolId))
    }

    fun favorites(): Flow<List<FavoriteEntity>> = favoriteDao.all()
    fun isFavorite(toolId: String): Flow<Boolean> = favoriteDao.isFavorite(toolId)
    suspend fun toggleFavorite(toolId: String, isNowFavorite: Boolean) {
        if (isNowFavorite) favoriteDao.add(FavoriteEntity(toolId))
        else favoriteDao.removeById(toolId)
    }
}
