package com.drs.nexus.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.drs.nexus.app.BuildConfig
import com.drs.nexus.app.R
import com.drs.nexus.app.data.AppContainer
import com.drs.nexus.app.navigation.Routes
import com.drs.nexus.app.ui.components.NexusTopBar
import com.drs.nexus.app.ui.theme.NexusShapes
import com.drs.nexus.core.data.AppLanguage
import com.drs.nexus.core.data.ThemeMode
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(container: AppContainer, navController: NavHostController) {
    val settings by container.settingsRepository.settings.collectAsState(initial = com.drs.nexus.core.data.UserSettings())
    val scope = rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        NexusTopBar(title = stringResource(R.string.settings_title))

        SettingsGroupTitle(stringResource(R.string.settings_appearance))
        SettingsCard {
            RadioRow(
                title = stringResource(R.string.theme_system),
                selected = settings.themeMode == ThemeMode.SYSTEM,
                onClick = { scope.launch { container.settingsRepository.setThemeMode(ThemeMode.SYSTEM) } }
            )
            RadioRow(
                title = stringResource(R.string.theme_light),
                selected = settings.themeMode == ThemeMode.LIGHT,
                onClick = { scope.launch { container.settingsRepository.setThemeMode(ThemeMode.LIGHT) } }
            )
            RadioRow(
                title = stringResource(R.string.theme_dark),
                selected = settings.themeMode == ThemeMode.DARK,
                onClick = { scope.launch { container.settingsRepository.setThemeMode(ThemeMode.DARK) } }
            )
            RadioRow(
                title = stringResource(R.string.theme_scheduled),
                selected = settings.themeMode == ThemeMode.SCHEDULED,
                onClick = { scope.launch { container.settingsRepository.setThemeMode(ThemeMode.SCHEDULED) } }
            )
            if (settings.themeMode == ThemeMode.SCHEDULED) {
                ScheduledWindowEditor(container, settings)
            }
        }
        SettingsGroupTitle(stringResource(R.string.settings_language))
        SettingsCard {
            RadioRow(
                title = stringResource(R.string.lang_system),
                selected = settings.language == AppLanguage.SYSTEM,
                onClick = { scope.launch { container.settingsRepository.setLanguage(AppLanguage.SYSTEM) } }
            )
            RadioRow(
                title = stringResource(R.string.lang_arabic),
                selected = settings.language == AppLanguage.ARABIC,
                onClick = { scope.launch { container.settingsRepository.setLanguage(AppLanguage.ARABIC) } }
            )
            RadioRow(
                title = stringResource(R.string.lang_english),
                selected = settings.language == AppLanguage.ENGLISH,
                onClick = { scope.launch { container.settingsRepository.setLanguage(AppLanguage.ENGLISH) } }
            )
        }

        SettingsGroupTitle(stringResource(R.string.settings_general))
        SettingsCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.settings_haptics),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        stringResource(R.string.settings_haptics_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = settings.hapticsEnabled,
                    onCheckedChange = { enabled -> scope.launch { container.settingsRepository.setHaptics(enabled) } },
                    colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                )
            }
        }

        SettingsGroupTitle(stringResource(R.string.settings_data))
        SettingsCard {
            com.drs.nexus.app.data.ExportHistoryRow(container)
            com.drs.nexus.app.data.BackupCreateRow(container)
            com.drs.nexus.app.data.BackupRestoreRow(container)
        }

        SettingsGroupTitle(stringResource(R.string.settings_about_section))
        SettingsCard {
            LinkRow(
                icon = Icons.Filled.Info,
                title = stringResource(R.string.about_title),
                subtitle = stringResource(R.string.about_tagline),
                onClick = { navController.navigate(Routes.ABOUT) }
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.settings_version),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.weight(1f))
                Text(
                    BuildConfig.VERSION_NAME,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

/**
 * HH:mm editors for the scheduled dark window. Input is validated live and
 * persisted only when both fields parse, so a typo can never disable the
 * feature silently.
 */
@Composable
private fun ScheduledWindowEditor(
    container: AppContainer,
    settings: com.drs.nexus.core.data.UserSettings
) {
    val scope = rememberCoroutineScope()
    var fromText by remember(settings.darkFrom) {
        mutableStateOf(settings.darkFrom)
    }
    var toText by remember(settings.darkTo) {
        mutableStateOf(settings.darkTo)
    }
    val fromValid = com.drs.nexus.core.util.ThemeSchedule.parseHm(fromText) != null
    val toValid = com.drs.nexus.core.util.ThemeSchedule.parseHm(toText) != null

    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            stringResource(R.string.theme_sched_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = fromText,
                onValueChange = { value ->
                    fromText = value
                    val f = com.drs.nexus.core.util.ThemeSchedule.parseHm(value)
                    val t = com.drs.nexus.core.util.ThemeSchedule.parseHm(toText)
                    if (f != null && t != null) {
                        scope.launch {
                            container.settingsRepository.setScheduledWindow(
                                com.drs.nexus.core.util.ThemeSchedule.formatHm(f),
                                com.drs.nexus.core.util.ThemeSchedule.formatHm(t)
                            )
                        }
                    }
                },
                label = { Text(stringResource(R.string.theme_sched_from)) },
                isError = !fromValid,
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = toText,
                onValueChange = { value ->
                    toText = value
                    val f = com.drs.nexus.core.util.ThemeSchedule.parseHm(fromText)
                    val t = com.drs.nexus.core.util.ThemeSchedule.parseHm(value)
                    if (f != null && t != null) {
                        scope.launch {
                            container.settingsRepository.setScheduledWindow(
                                com.drs.nexus.core.util.ThemeSchedule.formatHm(f),
                                com.drs.nexus.core.util.ThemeSchedule.formatHm(t)
                            )
                        }
                    }
                },
                label = { Text(stringResource(R.string.theme_sched_to)) },
                isError = !toValid,
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        if (!fromValid || !toValid) {
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.theme_sched_invalid),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun SettingsGroupTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = NexusShapes.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column { content() }
    }
}

@Composable
private fun RadioRow(title: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun LinkRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun AboutScreen(navController: NavHostController) {
    val context = LocalContext.current
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        NexusTopBar(title = stringResource(R.string.about_title), onBack = { navController.popBackStack() })
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = NexusShapes.card,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(R.string.about_tagline),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    stringResource(R.string.about_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/CTO-DRS/drs-nexus"))
                            )
                        }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.OpenInNew,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.about_repo),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    stringResource(R.string.about_license),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
