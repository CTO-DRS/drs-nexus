package com.drs.nexus.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.drs.nexus.app.R
import com.drs.nexus.app.data.AppContainer
import com.drs.nexus.app.navigation.Routes
import com.drs.nexus.app.ui.components.NexusTopBar
import com.drs.nexus.app.ui.components.SectionHeader
import com.drs.nexus.app.ui.components.ToolCard
import com.drs.nexus.app.ui.components.iconForKey
import com.drs.nexus.app.ui.components.localizedToolDesc
import com.drs.nexus.app.ui.components.localizedToolName
import com.drs.nexus.app.ui.theme.NexusShapes
import com.drs.nexus.app.ui.theme.categoryColor
import com.drs.nexus.app.ui.theme.nexusColorScheme
import com.drs.nexus.core.tools.ToolRegistry
import kotlinx.coroutines.launch

/**
 * Tools catalog: two persisted layouts (list / 2-column grid), real hardware
 * keyboard shortcuts ("/" or Ctrl+F focuses search, Esc clears) and live
 * bilingual search across every tool.
 */
@Composable
fun ToolsScreen(container: AppContainer, navController: NavHostController) {
    var query by remember { mutableStateOf("") }
    val dark = isSystemInDarkTheme()
    val scheme = nexusColorScheme(dark)
    val scope = rememberCoroutineScope()
    val settings by container.settingsRepository.settings.collectAsState(
        initial = com.drs.nexus.core.data.UserSettings()
    )
    val gridView = settings.toolsGridView

    // --- keyboard shortcut plumbing ---
    val rootFocus = remember { FocusRequester() }
    val searchFocus = remember { FocusRequester() }
    var searchFocused by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { if (!searchFocused) rootFocus.requestFocus() }

    fun handleShortcut(e: KeyEvent): Boolean {
        if (e.type != KeyEventType.KeyDown) return false
        return when {
            e.key == Key.Slash && !e.isCtrlPressed && !searchFocused -> {
                searchFocus.requestFocus(); true
            }
            e.isCtrlPressed && e.key == Key.F && !searchFocused -> {
                searchFocus.requestFocus(); true
            }
            e.key == Key.Escape && query.isNotEmpty() -> {
                query = ""
                if (searchFocused) rootFocus.requestFocus()
                true
            }
            else -> false
        }
    }

    // --- live bilingual filtering across all 23 tools ---
    val localizedNames = ToolRegistry.all.associate { it.nameKey to localizedToolName(it.nameKey) }
    val localizedDescs = ToolRegistry.all.associate { it.descKey to localizedToolDesc(it.descKey) }
    val visible = remember(query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) ToolRegistry.all
        else ToolRegistry.all.filter { tool ->
            (localizedNames[tool.nameKey] ?: "").lowercase().contains(q) ||
                (localizedDescs[tool.descKey] ?: "").lowercase().contains(q) ||
                tool.id.contains(q)
        }
    }

    val searchField: @Composable () -> Unit = {
        Column {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .focusRequester(searchFocus)
                    .onFocusChanged { searchFocused = it.isFocused },
                placeholder = { Text(stringResource(R.string.tools_search_hint), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                shape = NexusShapes.input,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
            if (query.isEmpty()) {
                Text(
                    stringResource(R.string.tools_search_shortcut_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 24.dp, top = 4.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    val topBar: @Composable () -> Unit = {
        NexusTopBar(
            title = stringResource(R.string.tools_title),
            trailing = {
                // Shows the layout you would switch TO.
                IconButton(
                    onClick = { scope.launch { container.settingsRepository.setToolsGridView(!gridView) } }
                ) {
                    Icon(
                        imageVector = if (gridView) Icons.Filled.Notes else Icons.Filled.GridView,
                        contentDescription = stringResource(
                            if (gridView) R.string.tools_view_list else R.string.tools_view_grid
                        ),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )
    }

    if (gridView) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(rootFocus)
                .focusable()
                .onPreviewKeyEvent { e -> handleShortcut(e) },
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) { topBar() }
            item(span = { GridItemSpan(maxLineSpan) }) { searchField() }
            if (visible.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) { ToolsEmpty() }
            } else {
                items(visible, key = { "grid_${it.id}" }) { tool ->
                    GridToolTile(
                        tool = tool,
                        name = localizedNames[tool.nameKey] ?: tool.id,
                        tint = categoryColor(tool.category.id, scheme),
                        onClick = { navController.navigate(Routes.tool(tool.id)) },
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(rootFocus)
                .focusable()
                .onPreviewKeyEvent { e -> handleShortcut(e) },
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item { topBar() }
            item { searchField() }
            if (visible.isEmpty()) {
                item { ToolsEmpty() }
            } else {
                ToolRegistry.Category.entries.forEach { category ->
                    val inCategory = visible.filter { it.category == category }
                    if (inCategory.isNotEmpty()) {
                        item(key = "header_${category.id}") {
                            SectionHeader(title = sectionTitle(category.id))
                        }
                        items(inCategory, key = { it.id }) { tool ->
                            val isFav by container.toolRepository.isFavorite(tool.id)
                                .collectAsState(initial = false)
                            ToolCard(
                                tool = tool,
                                name = localizedNames[tool.nameKey] ?: tool.id,
                                description = localizedDescs[tool.descKey] ?: "",
                                tint = categoryColor(tool.category.id, scheme),
                                isFavorite = isFav,
                                onFavoriteToggle = { scope.launch { container.toolRepository.toggleFavorite(tool.id, !isFav) } },
                                onClick = { navController.navigate(Routes.tool(tool.id)) },
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolsEmpty() {
    Text(
        stringResource(R.string.tools_empty),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(32.dp)
    )
}

/** Compact 2-column grid tile: icon + tool name. */
@Composable
private fun GridToolTile(
    tool: ToolRegistry.ToolDef,
    name: String,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = NexusShapes.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(tint.copy(alpha = 0.14f), NexusShapes.cardSmall),
                contentAlignment = Alignment.Center
            ) {
                Icon(iconForKey(tool.iconKey), contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                name,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun sectionTitle(categoryId: String): String = when (categoryId) {
    "developer" -> stringResource(R.string.section_developer)
    "converters" -> stringResource(R.string.section_converters)
    "generators" -> stringResource(R.string.section_generators)
    "calculators" -> stringResource(R.string.section_calculators)
    else -> stringResource(R.string.section_text)
}
