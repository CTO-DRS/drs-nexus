package com.drs.nexus.app

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.drs.nexus.app.navigation.NexusApp
import com.drs.nexus.app.ui.theme.NexusTheme
import com.drs.nexus.core.data.AppLanguage
import com.drs.nexus.core.data.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Locale

/**
 * Single-activity Compose app. Language is applied by wrapping the base context
 * so RTL layout direction and string resources both switch correctly.
 */
class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val language = readLanguagePreference(newBase)
        super.attachBaseContext(wrapWithLocale(newBase, language))
    }

    private fun readLanguagePreference(context: Context): String {
        // Blocking read is intentional and safe here: this runs before any UI is
        // created and reads a tiny preferences file, resolving within milliseconds.
        val app = application as? NexusApplication
        val stored = runCatching {
            runBlocking {
                val language = app?.container?.settingsRepository?.settings?.first()?.language
                    ?: AppLanguage.SYSTEM
                when (language) {
                    AppLanguage.SYSTEM -> ""
                    AppLanguage.ARABIC -> "ar"
                    AppLanguage.ENGLISH -> "en"
                }
            }
        }.getOrNull().orEmpty()
        return stored.ifEmpty { Locale.getDefault().language }
    }

    private fun wrapWithLocale(base: Context, language: String): Context {
        if (language.isBlank()) return base
        val locale = Locale.forLanguageTag(language)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return base.createConfigurationContext(config)
    }

    override fun onResume() {
        super.onResume()
        // Widget data may be stale while the app was in the background; refresh on return.
        com.drs.nexus.app.widget.NexusWidgetProvider.updateAll(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val container = (application as NexusApplication).container
        setContent {
            val settings by container.settingsRepository.settings.collectAsState(initial = null)
            val s = settings
            val dark = when (s?.themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                else -> (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            }
            NexusTheme(darkTheme = dark) {
                NexusApp(container = container)
            }
        }
    }
}
