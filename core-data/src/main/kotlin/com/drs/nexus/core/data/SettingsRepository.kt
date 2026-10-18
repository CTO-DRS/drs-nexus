package com.drs.nexus.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "nexus_settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class AppLanguage { SYSTEM, ARABIC, ENGLISH }

data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val onboardingDone: Boolean = false,
    val hapticsEnabled: Boolean = true
)

/** Real DataStore-backed settings repository. */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val LANGUAGE = stringPreferencesKey("app_language")
        val ONBOARDING = booleanPreferencesKey("onboarding_done")
        val HAPTICS = booleanPreferencesKey("haptics_enabled")
    }

    val settings: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            themeMode = prefs[Keys.THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
            language = prefs[Keys.LANGUAGE]?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() } ?: AppLanguage.SYSTEM,
            onboardingDone = prefs[Keys.ONBOARDING] ?: false,
            hapticsEnabled = prefs[Keys.HAPTICS] ?: true
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

    fun favorites(): Flow<List<FavoriteEntity>> = favoriteDao.all()
    fun isFavorite(toolId: String): Flow<Boolean> = favoriteDao.isFavorite(toolId)
    suspend fun toggleFavorite(toolId: String, isNowFavorite: Boolean) {
        if (isNowFavorite) favoriteDao.add(FavoriteEntity(toolId))
        else favoriteDao.removeById(toolId)
    }
}
