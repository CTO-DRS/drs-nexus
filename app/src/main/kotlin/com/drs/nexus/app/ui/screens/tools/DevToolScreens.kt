package com.drs.nexus.app.ui.screens.tools

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.drs.nexus.app.R
import com.drs.nexus.app.data.AppContainer
import com.drs.nexus.app.ui.components.ResultActionsRow
import com.drs.nexus.app.ui.components.ResultCard
import com.drs.nexus.app.ui.theme.nexusColorScheme
import com.drs.nexus.core.tools.Base64Tool
import com.drs.nexus.core.tools.HashTool
import com.drs.nexus.core.tools.JsonFormatterTool
import com.drs.nexus.core.tools.RegexTesterTool
import com.drs.nexus.core.tools.UrlTool
import kotlinx.coroutines.launch

// ---------------- JSON Formatter ----------------

@Composable
fun JsonToolScreen(container: AppContainer, navController: NavHostController) {
    val tool = com.drs.nexus.core.tools.ToolRegistry.byId("json_formatter")!!
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<JsonFormatterTool.JsonResult?>(null) }
    val fmt = remember { JsonFormatterTool() }
    val modeLabels = listOf(
        stringResource(R.string.action_format),
        stringResource(R.string.action_minify),
        stringResource(R.string.action_validate)
    )
    var mode by remember { mutableStateOf(0) }

    ToolScaffold(tool, container, navController) {
        OptionChipRow(options = modeLabels, selectedIndex = mode, onSelect = { mode = it })
        ToolInputField(value = input, onValueChange = { input = it; result = null })
        RunButton(stringResource(R.string.action_run)) {
            val r = fmt.format(input)
            result = r
            val outText = when (r) {
                is JsonFormatterTool.JsonResult.Success ->
                    if (mode == 1) r.minified else r.formatted
                is JsonFormatterTool.JsonResult.Failure -> "Error: ${r.message} (${r.line}:${r.column})"
            }
            scope.launch { container.toolRepository.logRun(tool.id, input.take(120), outText.take(120)) }
        }
        result?.let { r ->
            when (r) {
                is JsonFormatterTool.JsonResult.Success -> {
                    if (mode == 2) {
                        ResultCard(
                            title = stringResource(R.string.action_validate) + " ✓",
                            content = "keys=${r.stats.keys} · depth=${r.stats.maxDepth} · objects=${r.stats.objects} · arrays=${r.stats.arrays} · bytes=${r.stats.sizeBytes}"
                        )
                    } else {
                        val text = if (mode == 1) r.minified else r.formatted
                        ResultCard(title = stringResource(R.string.label_result), content = text)
                        ResultActionsRow(result = text)
                    }
                    if (mode == 0) {
                        Text(
                            "keys=${r.stats.keys} · depth=${r.stats.maxDepth} · objects=${r.stats.objects} · arrays=${r.stats.arrays}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                is JsonFormatterTool.JsonResult.Failure -> ErrorBanner("${r.message} — line ${r.line}, column ${r.column}")
            }
        }
    }
}

// ---------------- Base64 ----------------

@Composable
fun Base64ToolScreen(container: AppContainer, navController: NavHostController) {
    val tool = com.drs.nexus.core.tools.ToolRegistry.byId("base64")!!
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var output by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val b64 = remember { Base64Tool() }
    val modeLabels = listOf(stringResource(R.string.action_encode), stringResource(R.string.action_decode))
    var mode by remember { mutableStateOf(0) }
    var urlSafe by remember { mutableStateOf(false) }
    val optionLabels = listOf("URL-safe")
    var urlSafeIndex by remember { mutableStateOf(-1) }

    ToolScaffold(tool, container, navController) {
        OptionChipRow(options = modeLabels, selectedIndex = mode, onSelect = { mode = it; output = null; error = null })
        OptionChipRow(
            options = optionLabels,
            selectedIndex = urlSafeIndex,
            onSelect = { urlSafeIndex = if (urlSafeIndex == 0) -1 else 0; urlSafe = urlSafeIndex == 0 }
        )
        ToolInputField(value = input, onValueChange = { input = it; output = null; error = null })
        RunButton(stringResource(R.string.action_run)) {
            error = null
            if (input.isBlank()) {
                error = "Please enter some input first"
            } else {
                when (mode) {
                    0 -> {
                        output = b64.encode(input, urlSafe = urlSafe)
                        scope.launch { container.toolRepository.logRun(tool.id, input.take(120), output!!.take(120)) }
                    }
                    else -> {
                        when (val r = b64.decode(input, urlSafe = urlSafe)) {
                            is Base64Tool.Base64Result.Success -> {
                                output = r.text
                                scope.launch { container.toolRepository.logRun(tool.id, input.take(120), r.text.take(120)) }
                            }
                            is Base64Tool.Base64Result.Failure -> error = r.message
                        }
                    }
                }
            }
        }
        error?.let { ErrorBanner(it) }
        output?.let { out ->
            ResultCard(title = stringResource(R.string.label_result), content = out)
            ResultActionsRow(result = out)
        }
    }
}

// ---------------- Hash Calculator ----------------

@Composable
fun HashToolScreen(container: AppContainer, navController: NavHostController) {
    val tool = com.drs.nexus.core.tools.ToolRegistry.byId("hash")!!
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var hashes by remember { mutableStateOf<Map<HashTool.Algorithm, String>?>(null) }
    val hashTool = remember { HashTool() }

    ToolScaffold(tool, container, navController) {
        ToolInputField(value = input, onValueChange = { input = it; hashes = null })
        RunButton(stringResource(R.string.action_run)) {
            if (input.isNotEmpty()) {
                hashes = hashTool.hashAll(input)
                scope.launch {
                    container.toolRepository.logRun(tool.id, input.take(120), hashes!![HashTool.Algorithm.SHA_256]!!.take(120))
                }
            }
        }
        hashes?.forEach { (algo, value) ->
            Column {
                Text(algo.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                ResultCard(title = algo.label, content = value)
                ResultActionsRow(result = value)
            }
        }
    }
}

// ---------------- Regex Tester ----------------

@Composable
fun RegexToolScreen(container: AppContainer, navController: NavHostController) {
    val tool = com.drs.nexus.core.tools.ToolRegistry.byId("regex_tester")!!
    val scope = rememberCoroutineScope()
    var pattern by remember { mutableStateOf("") }
    var testInput by remember { mutableStateOf("") }
    var replacement by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<RegexTesterTool.RegexResult?>(null) }
    val tester = remember { RegexTesterTool() }
    val flags = listOf("i", "m", "s")
    var flagIndexes by remember { mutableStateOf(setOf<Int>()) }

    ToolScaffold(tool, container, navController) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            flags.forEachIndexed { i, flag ->
                androidx.compose.material3.FilterChip(
                    selected = i in flagIndexes,
                    onClick = {
                        flagIndexes = if (i in flagIndexes) flagIndexes - i else flagIndexes + i
                    },
                    label = { Text("regex option: $flag", style = MaterialTheme.typography.labelMedium) },
                    shape = com.drs.nexus.app.ui.theme.NexusShapes.cardSmall
                )
            }
        }
        ToolInputField(
            value = pattern,
            onValueChange = { pattern = it; result = null },
            placeholderText = "pattern  e.g.  \\d+"
        )
        ToolInputField(
            value = testInput,
            onValueChange = { testInput = it; result = null },
            placeholderText = "test string"
        )
        RunButton(stringResource(R.string.action_run)) {
            result = tester.test(
                pattern = pattern,
                input = testInput,
                caseInsensitive = 0 in flagIndexes,
                multiline = 1 in flagIndexes,
                dotAll = 2 in flagIndexes,
                replacement = replacement
            )
            val out = when (val r = result) {
                is RegexTesterTool.RegexResult.Success -> "matches=${r.matchCount}"
                is RegexTesterTool.RegexResult.Failure -> "error: ${r.message}"
                null -> ""
            }
            scope.launch { container.toolRepository.logRun(tool.id, pattern.take(120), out) }
        }
        ToolInputField(
            value = replacement,
            onValueChange = { replacement = it },
            placeholderText = "replacement (optional)",
            minLines = 1,
            maxLines = 2
        )
        when (val r = result) {
            is RegexTesterTool.RegexResult.Success -> {
                Text(
                    stringResource(R.string.label_result) + ": ${r.matchCount} matches",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                r.matches.take(20).forEach { m ->
                    ResultCard(
                        title = "[${m.start}..${m.end})",
                        content = m.value
                    )
                }
                if (r.replacePreview.isNotEmpty()) {
                    ResultCard(title = "Replaced", content = r.replacePreview)
                    ResultActionsRow(result = r.replacePreview)
                }
            }
            is RegexTesterTool.RegexResult.Failure -> ErrorBanner(r.message)
            null -> {}
        }
    }
}

// ---------------- URL Encode/Decode ----------------

@Composable
fun UrlToolScreen(container: AppContainer, navController: NavHostController) {
    val tool = com.drs.nexus.core.tools.ToolRegistry.byId("url_codec")!!
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var output by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val urlTool = remember { UrlTool() }
    val modeLabels = listOf(stringResource(R.string.action_encode), stringResource(R.string.action_decode))
    var mode by remember { mutableStateOf(0) }

    ToolScaffold(tool, container, navController) {
        OptionChipRow(options = modeLabels, selectedIndex = mode, onSelect = { mode = it; output = null; error = null })
        ToolInputField(value = input, onValueChange = { input = it; output = null; error = null })
        RunButton(stringResource(R.string.action_run)) {
            error = null
            when (mode) {
                0 -> {
                    output = urlTool.encode(input)
                    scope.launch { container.toolRepository.logRun(tool.id, input.take(120), output!!.take(120)) }
                }
                else -> when (val r = urlTool.decode(input)) {
                    is UrlTool.UrlResult.Success -> {
                        output = r.text
                        scope.launch { container.toolRepository.logRun(tool.id, input.take(120), r.text.take(120)) }
                    }
                    is UrlTool.UrlResult.Failure -> error = r.message
                }
            }
        }
        error?.let { ErrorBanner(it) }
        output?.let { out ->
            ResultCard(title = stringResource(R.string.label_result), content = out)
            ResultActionsRow(result = out)
        }
    }
}
