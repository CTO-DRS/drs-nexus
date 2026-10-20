package com.drs.nexus.app.ui.screens

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
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.drs.nexus.app.R
import com.drs.nexus.app.data.AppContainer
import com.drs.nexus.app.ui.components.EmptyState
import com.drs.nexus.app.ui.components.NexusTopBar
import com.drs.nexus.app.ui.components.iconForKey
import com.drs.nexus.app.ui.components.localizedToolName
import com.drs.nexus.app.ui.theme.NexusShapes
import com.drs.nexus.core.data.ToolRunEntity
import com.drs.nexus.core.tools.ToolRegistry
import com.drs.nexus.core.util.RunStamp
import com.drs.nexus.core.util.UsageStats
import com.drs.nexus.core.util.UsageSummary

/**
 * Usage insights — aggregate view over the real Room history using the pure
 * UsageStats engine from core-common. Read-only; nothing here mutates data.
 */
@Composable
fun UsageStatsScreen(container: AppContainer, navController: NavHostController) {
    var runs by remember { mutableStateOf<List<ToolRunEntity>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        runs = container.toolRepository.allRunsOnce()
        loaded = true
    }

    Column(Modifier.fillMaxSize()) {
        NexusTopBar(
            title = stringResource(R.string.usage_title),
            onBack = { navController.popBackStack() }
        )

        if (loaded && runs.isEmpty()) {
            EmptyState(Icons.Filled.QueryStats, stringResource(R.string.usage_empty))
            return@Column
        }

        val now = remember(runs) { System.currentTimeMillis() }
        val summary = remember(runs, now) {
            UsageStats.summarize(runs.map { RunStamp(it.toolId, it.executedAt) }, now)
        }

        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatTile(
                        value = summary.totalRuns,
                        label = stringResource(R.string.usage_total),
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        value = summary.distinctTools,
                        label = stringResource(R.string.usage_distinct),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item {
                WeeklyCard(summary)
            }
            if (summary.top.isNotEmpty()) {
                item {
                    Text(
                        stringResource(R.string.usage_top_tools),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                    )
                }
                val maxRuns = summary.top.first().runs.coerceAtLeast(1)
                items(summary.top.size) { index ->
                    val usage = summary.top[index]
                    ToolUsageRow(
                        toolId = usage.toolId,
                        runs = usage.runs,
                        fraction = usage.runs.toFloat() / maxRuns,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatTile(value: Int, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = NexusShapes.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                value.toString(),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(2.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WeeklyCard(summary: UsageSummary) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = NexusShapes.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.usage_week_now),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        summary.last7Days.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.usage_week_prev),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        summary.previous7Days.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            when {
                summary.weeklyDelta > 0 -> TrendRow(
                    icon = { Icon(Icons.Filled.ArrowUpward, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp)) },
                    text = stringResource(R.string.usage_trend_up, summary.weeklyDelta)
                )
                summary.weeklyDelta < 0 -> TrendRow(
                    icon = { Icon(Icons.Filled.ArrowDownward, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp)) },
                    text = stringResource(R.string.usage_trend_down, -summary.weeklyDelta)
                )
                summary.totalRuns > 0 -> TrendRow(
                    icon = { Icon(Icons.Filled.Remove, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp)) },
                    text = stringResource(R.string.usage_trend_flat)
                )
            }
        }
    }
}

@Composable
private fun TrendRow(icon: @Composable () -> Unit, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        icon()
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ToolUsageRow(toolId: String, runs: Int, fraction: Float, modifier: Modifier = Modifier) {
    val tool = ToolRegistry.byId(toolId)
    val name = tool?.let { localizedToolName(it.nameKey) } ?: toolId
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = NexusShapes.cardSmall,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = iconForKey(tool?.iconKey ?: "notes"),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.usage_runs_value, runs),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { fraction.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
