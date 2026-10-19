package com.drs.nexus.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.res.stringResource
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
import androidx.compose.foundation.isSystemInDarkTheme
import com.drs.nexus.core.tools.ToolRegistry
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
fun ToolsScreen(container: AppContainer, navController: NavHostController) {
    var query by remember { mutableStateOf("") }
    val dark = isSystemInDarkTheme()
    val scheme = nexusColorScheme(dark)
    val scope = rememberCoroutineScope()

    val filtered = remember(query) {
        val q = query.trim().lowercase()
        ToolRegistry.all.filter { q.isEmpty() || it.id.contains(q) }
    }

    // Localized names/descriptions for ALL tools via the unified resolver
    val names = ToolRegistry.all.associate { it.nameKey to localizedToolName(it.nameKey) }
    val descs = ToolRegistry.all.associate { it.descKey to localizedToolDesc(it.descKey) }
    // Filter on localized names too (real bilingual search)
    val visible = remember(filtered, query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) filtered
        else filtered.filter { tool ->
            (names[tool.nameKey] ?: "").lowercase().contains(q) ||
                (descs[tool.descKey] ?: "").lowercase().contains(q) ||
                tool.id.contains(q)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            NexusTopBar(title = stringResource(R.string.tools_title))
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
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
            Spacer(Modifier.height(8.dp))
        }
        if (visible.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.tools_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(32.dp)
                )
            }
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
                            name = names[tool.nameKey] ?: tool.id,
                            description = descs[tool.descKey] ?: "",
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

@Composable
private fun sectionTitle(categoryId: String): String = when (categoryId) {
    "developer" -> stringResource(R.string.section_developer)
    "converters" -> stringResource(R.string.section_converters)
    "generators" -> stringResource(R.string.section_generators)
    "calculators" -> stringResource(R.string.section_calculators)
    else -> stringResource(R.string.section_text)
}
