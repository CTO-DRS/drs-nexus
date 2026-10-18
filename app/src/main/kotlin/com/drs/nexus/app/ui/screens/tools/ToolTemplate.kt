package com.drs.nexus.app.ui.screens.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.drs.nexus.app.R
import com.drs.nexus.app.data.AppContainer
import com.drs.nexus.app.ui.components.NexusTopBar
import com.drs.nexus.app.ui.components.ResultActionsRow
import com.drs.nexus.app.ui.components.iconForKey
import com.drs.nexus.app.ui.theme.NexusShapes
import com.drs.nexus.app.ui.theme.categoryColor
import com.drs.nexus.app.ui.theme.nexusColorScheme
import com.drs.nexus.core.tools.ToolRegistry
import kotlinx.coroutines.launch

/** Common scaffold for every tool screen: top bar, favorite star, content slot. */
@Composable
fun ToolScaffold(
    tool: ToolRegistry.ToolDef,
    container: AppContainer,
    navController: NavHostController,
    content: @Composable () -> Unit
) {
    val dark = isSystemInDarkTheme()
    val scheme = nexusColorScheme(dark)
    val tint = categoryColor(tool.category.id, scheme)
    val isFav by container.toolRepository.isFavorite(tool.id).collectAsState(initial = false)
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize()) {
        NexusTopBar(
            title = toolTitle(tool.nameKey),
            subtitle = toolDescription(tool.descKey),
            onBack = { navController.popBackStack() },
            trailing = {
                IconButton(onClick = { scope.launch { container.toolRepository.toggleFavorite(tool.id, !isFav) } }) {
                    Icon(
                        if (isFav) Icons.Filled.Star else Icons.Outlined.StarOutline,
                        contentDescription = null,
                        tint = if (isFav) Color(0xFFFFB800) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { content() }
        }
    }
}

@Composable
fun toolTitle(nameKey: String): String = when (nameKey) {
    "tool_json_name" -> stringResource(R.string.tool_json_name)
    "tool_base64_name" -> stringResource(R.string.tool_base64_name)
    "tool_hash_name" -> stringResource(R.string.tool_hash_name)
    "tool_regex_name" -> stringResource(R.string.tool_regex_name)
    "tool_url_name" -> stringResource(R.string.tool_url_name)
    "tool_ts_name" -> stringResource(R.string.tool_ts_name)
    "tool_unit_name" -> stringResource(R.string.tool_unit_name)
    "tool_uuid_name" -> stringResource(R.string.tool_uuid_name)
    "tool_pass_name" -> stringResource(R.string.tool_pass_name)
    "tool_lorem_name" -> stringResource(R.string.tool_lorem_name)
    else -> stringResource(R.string.tool_text_name)
}

@Composable
fun toolDescription(descKey: String): String = when (descKey) {
    "tool_json_desc" -> stringResource(R.string.tool_json_desc)
    "tool_base64_desc" -> stringResource(R.string.tool_base64_desc)
    "tool_hash_desc" -> stringResource(R.string.tool_hash_desc)
    "tool_regex_desc" -> stringResource(R.string.tool_regex_desc)
    "tool_url_desc" -> stringResource(R.string.tool_url_desc)
    "tool_ts_desc" -> stringResource(R.string.tool_ts_desc)
    "tool_unit_desc" -> stringResource(R.string.tool_unit_desc)
    "tool_uuid_desc" -> stringResource(R.string.tool_uuid_desc)
    "tool_pass_desc" -> stringResource(R.string.tool_pass_desc)
    "tool_lorem_desc" -> stringResource(R.string.tool_lorem_desc)
    else -> stringResource(R.string.tool_text_desc)
}

/** Multiline input styled like an iOS text editor. */
@Composable
fun ToolInputField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    minLines: Int = 5,
    maxLines: Int = 10,
    placeholderText: String = stringResource(R.string.hint_input)
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        minLines = minLines,
        maxLines = maxLines,
        shape = NexusShapes.input,
        placeholder = { Text(placeholderText, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface
        ),
        textStyle = MaterialTheme.typography.bodyMedium
    )
}

/** Primary run button. */
@Composable
fun RunButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = NexusShapes.button,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

/** Horizontal group of real FilterChips for tool options. */
@Composable
fun OptionChipRow(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEachIndexed { i, option ->
            FilterChip(
                selected = selectedIndex == i,
                onClick = { onSelect(i) },
                label = { Text(option, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                shape = NexusShapes.cardSmall,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

/** Red error banner for genuine tool errors. */
@Composable
fun ErrorBanner(message: String) {
    Card(
        shape = NexusShapes.cardSmall,
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFF453A).copy(alpha = 0.12f))
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Error, contentDescription = null, tint = Color(0xFFFF453A), modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFE5484D)
            )
        }
    }
}

/** Router that maps a persisted toolId to its real screen. */
@Composable
fun ToolDetailScreen(toolId: String, container: AppContainer, navController: NavHostController) {
    when (toolId) {
        "json_formatter" -> JsonToolScreen(container, navController)
        "base64" -> Base64ToolScreen(container, navController)
        "hash" -> HashToolScreen(container, navController)
        "regex_tester" -> RegexToolScreen(container, navController)
        "url_codec" -> UrlToolScreen(container, navController)
        "timestamp" -> TimestampToolScreen(container, navController)
        "unit_converter" -> UnitConverterToolScreen(container, navController)
        "uuid" -> UuidToolScreen(container, navController)
        "password" -> PasswordToolScreen(container, navController)
        "lorem" -> LoremToolScreen(container, navController)
        "text_utils" -> TextUtilsToolScreen(container, navController)
        else -> JsonToolScreen(container, navController)
    }
}
