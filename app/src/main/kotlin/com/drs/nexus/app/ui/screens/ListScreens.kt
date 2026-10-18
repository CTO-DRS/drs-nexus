package com.drs.nexus.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.drs.nexus.app.navigation.Routes
import com.drs.nexus.app.ui.components.DeleteIconButton
import com.drs.nexus.app.ui.components.EmptyState
import com.drs.nexus.app.ui.components.NexusTopBar
import com.drs.nexus.app.ui.components.ToolCard
import com.drs.nexus.app.ui.theme.NexusShapes
import com.drs.nexus.app.ui.theme.categoryColor
import com.drs.nexus.app.ui.theme.nexusColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import com.drs.nexus.core.data.ToolRunEntity
import com.drs.nexus.core.tools.ToolRegistry
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
private fun toolName(nameKey: String): String = when (nameKey) {
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
    "tool_text_name" -> stringResource(R.string.tool_text_name)
    else -> nameKey
}

@Composable
fun HistoryScreen(container: AppContainer, navController: NavHostController) {
    val runs by container.toolRepository.recentRuns(limit = 200).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize()) {
        NexusTopBar(
            title = stringResource(R.string.history_title),
            trailing = {
                if (runs.isNotEmpty()) {
                    TextButton(onClick = { scope.launch { container.toolRepository.clearHistory() } }) {
                        Text(
                            stringResource(R.string.history_clear),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        )
        if (runs.isEmpty()) {
            EmptyState(Icons.Filled.History, stringResource(R.string.history_empty))
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                items(runs, key = { it.id }) { run ->
                    HistoryRunCard(
                        run = run,
                        onClick = { navController.navigate(Routes.tool(run.toolId)) },
                        onDelete = { scope.launch { container.toolRepository.deleteRun(run.id) } },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryRunCard(run: ToolRunEntity, onClick: () -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    val tool = ToolRegistry.byId(run.toolId)
    val name = tool?.let { toolName(it.nameKey) } ?: run.toolId
    val timeFormat = remember { SimpleDateFormat("EEE, MMM d · HH:mm", Locale.getDefault()) }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = NexusShapes.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        timeFormat.format(Date(run.executedAt)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.padding(2.dp))
                Text(
                    "→ " + run.outputPreview.ifBlank { "—" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(10.dp))
            DeleteIconButton(onClick = onDelete)
        }
    }
}

@Composable
fun FavoritesScreen(container: AppContainer, navController: NavHostController) {
    val favorites by container.toolRepository.favorites().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val dark = isSystemInDarkTheme()
    val scheme = nexusColorScheme(dark)

    val favTools = favorites.mapNotNull { fav -> ToolRegistry.byId(fav.toolId) }

    Column(Modifier.fillMaxSize()) {
        NexusTopBar(title = stringResource(R.string.favorites_title))
        if (favTools.isEmpty()) {
            EmptyState(Icons.Filled.Star, stringResource(R.string.favorites_empty))
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                items(favTools, key = { it.id }) { tool ->
                    val name = toolName(tool.nameKey)
                    val desc = when (tool.descKey) {
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
                    ToolCard(
                        tool = tool,
                        name = name,
                        description = desc,
                        tint = categoryColor(tool.category.id, scheme),
                        isFavorite = true,
                        onFavoriteToggle = { scope.launch { container.toolRepository.toggleFavorite(tool.id, false) } },
                        onClick = { navController.navigate(Routes.tool(tool.id)) },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}
