package com.drs.nexus.app

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.drs.nexus.app.navigation.NexusApp
import com.drs.nexus.app.security.AppLockController
import com.drs.nexus.app.ui.screens.AppLockOverlay
import com.drs.nexus.app.ui.theme.NexusTheme
import com.drs.nexus.core.data.AppLanguage
import com.drs.nexus.core.data.ThemeMode
import com.drs.nexus.core.util.ThemeSchedule
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Locale

/**
 * Single-activity Compose app. Language is applied by wrapping the base context
 * so RTL layout direction and string resources both switch correctly.
 *
 * Extends FragmentActivity (not ComponentActivity directly) because the
 * androidx.biometric app lock requires it.
 */
class MainActivity : FragmentActivity() {

    private val lockController by lazy { AppLockController(this) }
    private var backgroundedAtMs: Long? = null
    private var cachedAppLockEnabled = false

    // Compose state so the UI can gate rendering until the lock decision is known.
    private var initialLockEvaluated by androidx.compose.runtime.mutableStateOf(false)

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

    override fun onStart() {
        super.onStart()
        // Warm return from background. Cold start is handled after the first
        // settings snapshot arrives (composition effect below).
        if (initialLockEvaluated) {
            lockController.evaluateForeground(cachedAppLockEnabled, backgroundedAtMs)
        }
        backgroundedAtMs = null
    }

    override fun onStop() {
        super.onStop()
        backgroundedAtMs = System.currentTimeMillis()
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
            // Scheduled dark mode must flip by itself while the app stays open;
            // a one-minute tick recomputes the decision without user interaction.
            var minuteTick by remember { mutableStateOf(0) }
            LaunchedEffect(Unit) {
                while (true) {
                    delay(60_000L)
                    minuteTick++
                }
            }
            val dark = remember(s?.themeMode, s?.darkFrom, s?.darkTo, minuteTick) {
                when (s?.themeMode) {
                    ThemeMode.DARK -> true
                    ThemeMode.LIGHT -> false
                    ThemeMode.SCHEDULED -> ThemeSchedule.isDarkNow(
                        System.currentTimeMillis(),
                        s?.darkFrom ?: "19:00",
                        s?.darkTo ?: "06:00"
                    )
                    else -> (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                }
            }
            // Cold-start lock evaluation happens as soon as real settings arrive.
            // The "null" initial snapshot is skipped — locking must use real prefs.
            LaunchedEffect(s?.appLockEnabled) {
                val enabled = s?.appLockEnabled ?: return@LaunchedEffect
                cachedAppLockEnabled = enabled
                if (!initialLockEvaluated) {
                    initialLockEvaluated = true
                    lockController.evaluateForeground(enabled, null)
                }
            }
            // Material You accent — takes the wallpaper-derived color on Android 12+.
            val context = LocalContext.current
            val dynamicAccent = remember(s?.dynamicColorEnabled, dark) {
                if (s?.dynamicColorEnabled == true && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (dark) dynamicDarkColorScheme(context).primary
                    else dynamicLightColorScheme(context).primary
                } else null
            }
            Box(Modifier.fillMaxSize()) {
                if (initialLockEvaluated) {
                    NexusTheme(darkTheme = dark, dynamicAccent = dynamicAccent) {
                        NexusApp(container = container)
                    }
                }
                // The lock gate sits on top of everything, stays fully opaque, and
                // also covers the brief cold-start window before settings load so
                // protected content is never revealed.
                if (lockController.state.locked) {
                    AppLockOverlay(
                        controller = lockController,
                        promptTitle = getString(com.drs.nexus.app.R.string.applock_prompt_title),
                        promptSubtitle = getString(com.drs.nexus.app.R.string.applock_prompt_subtitle)
                    )
                    // Show the system prompt as soon as the activity reaches RESUMED.
                    DisposableEffect(Unit) {
                        val observer = LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_RESUME && lockController.state.locked) {
                                lockController.authenticate(
                                    title = getString(com.drs.nexus.app.R.string.applock_prompt_title),
                                    subtitle = getString(com.drs.nexus.app.R.string.applock_prompt_subtitle)
                                )
                            }
                        }
                        lifecycle.addObserver(observer)
                        onDispose { lifecycle.removeObserver(observer) }
                    }
                } else if (!initialLockEvaluated) {
                    com.drs.nexus.app.ui.screens.BlankLockCover()
                }
            }
        }
    }
}
