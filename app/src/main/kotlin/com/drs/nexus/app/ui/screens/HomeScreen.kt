package com.drs.nexus.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.drs.nexus.app.ui.components.EmptyState
import com.drs.nexus.app.ui.components.NexusTopBar
import com.drs.nexus.app.ui.components.iconForKey
import com.drs.nexus.app.ui.components.localizedToolName
import com.drs.nexus.app.ui.theme.NexusPalette
import com.drs.nexus.app.ui.theme.NexusShapes
import com.drs.nexus.app.ui.theme.categoryColor
import com.drs.nexus.app.ui.theme.nexusColorScheme
import com.drs.nexus.core.data.ToolRunEntity
import com.drs.nexus.core.tools.ToolRegistry
import androidx.compose.foundation.isSystemInDarkTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(container: AppContainer, navController: NavHostController) {
    val dark = isSystemInDarkTheme()
    val scheme = nexusColorScheme(dark)
    val runs by container.toolRepository.recentRuns(limit = 8).collectAsState(initial = emptyList())
    val favorites by container.toolRepository.favorites().collectAsState(initial = emptyList())

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
    ) {
        item {
            NexusTopBar(
                title = stringResource(R.string.home_greeting),
                subtitle = stringResource(R.string.home_subtitle)
            )
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    value = ToolRegistry.all.size.toString(),
                    label = stringResource(R.string.home_stats_tools),
                    tint = scheme.accent,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    value = runs.size.toString(),
                    label = stringResource(R.string.home_stats_runs),
                    tint = NexusPalette.Indigo,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    value = favorites.size.toString(),
                    label = stringResource(R.string.home_stats_favorites),
                    tint = NexusPalette.Purple,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickTile(
                    icon = Icons.Filled.Apps,
                    label = stringResource(R.string.nav_tools),
                    tint = scheme.accent,
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate(Routes.TOOLS) }
                )
                QuickTile(
                    icon = Icons.Filled.Favorite,
                    label = stringResource(R.string.favorites_title),
                    tint = NexusPalette.Purple,
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate(Routes.FAVORITES) }
                )
            }
        }
        item {
            Text(
                stringResource(R.string.home_recent_activity),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }
        if (runs.isEmpty()) {
            item {
                EmptyState(Icons.Filled.History, stringResource(R.string.home_no_recent))
            }
        } else {
            items(runs, key = { it.id }) { run ->
                RecentRunCard(
                    run = run,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp),
                    onClick = { navController.navigate(Routes.tool(run.toolId)) }
                )
            }
        }
        if (favorites.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.home_quick_tools),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
        } else {
            item {
                Text(
                    stringResource(R.string.home_pinned_favorites),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
        }
        // Pinned favorites win over the static quick list; fall back when empty.
        val quick = if (favorites.isEmpty()) ToolRegistry.all.take(4)
        else favorites.take(4).mapNotNull { ToolRegistry.byId(it.toolId) }
        items(quick.size) { i ->
            val tool = quick[i]
            QuickToolRow(
                tool = tool,
                name = localizedToolName(tool.nameKey),
                tint = categoryColor(tool.category.id, scheme),
                onClick = { navController.navigate(Routes.tool(tool.id)) }
            )
        }
    }
}

@Composable
private fun StatCard(value: String, label: String, tint: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = NexusShapes.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.headlineMedium, color = tint)
            Spacer(Modifier.height(2.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun QuickTile(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tint: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, NexusShapes.card)
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier.size(34.dp).background(tint.copy(alpha = 0.14f), NexusShapes.cardSmall),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        }
        Text(label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun RecentRunCard(run: ToolRunEntity, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val tool = ToolRegistry.byId(run.toolId)
    val name = tool?.let { localizedToolName(it.nameKey) } ?: run.toolId
    val dark = isSystemInDarkTheme()
    val tint = categoryColor(run.toolId, nexusColorScheme(dark))
    val timeFormat = remember { SimpleDateFormat("MMM d · HH:mm", Locale.getDefault()) }
    Card(
        modifier = modifier.fillMaxWidth().clickable { onClick() },
        shape = NexusShapes.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(34.dp).background(tint.copy(alpha = 0.14f), NexusShapes.cardSmall),
                contentAlignment = Alignment.Center
            ) {
                Icon(iconForKey(tool?.iconKey ?: "text_fields"), contentDescription = null, tint = tint, modifier = Modifier.size(17.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    run.inputPreview.ifBlank { "—" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                timeFormat.format(Date(run.executedAt)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuickToolRow(tool: ToolRegistry.ToolDef, name: String, tint: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .background(MaterialTheme.colorScheme.surface, NexusShapes.card)
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(32.dp).background(tint.copy(alpha = 0.14f), NexusShapes.cardSmall),
            contentAlignment = Alignment.Center
        ) {
            Icon(iconForKey(tool.iconKey), contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.weight(1f))
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}
