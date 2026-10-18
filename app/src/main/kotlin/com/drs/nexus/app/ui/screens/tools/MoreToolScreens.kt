package com.drs.nexus.app.ui.screens.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.drs.nexus.app.ui.theme.NexusShapes
import com.drs.nexus.core.tools.LoremIpsumTool
import com.drs.nexus.core.tools.PasswordGenerator
import com.drs.nexus.core.tools.TextUtilitiesTool
import com.drs.nexus.core.tools.TimestampConverterTool
import com.drs.nexus.core.tools.UnitConverterTool
import com.drs.nexus.core.tools.UuidTool
import kotlinx.coroutines.launch

// ---------------- Timestamp Converter ----------------

@Composable
fun TimestampToolScreen(container: AppContainer, navController: NavHostController) {
    val tool = com.drs.nexus.core.tools.ToolRegistry.byId("timestamp")!!
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<TimestampConverterTool.TsResult?>(null) }
    var direction by remember { mutableStateOf(0) }
    val conv = remember { TimestampConverterTool() }
    val zones = listOf("UTC", "Asia/Riyadh", "Africa/Cairo", "Europe/London", "America/New_York")
    var zoneIndex by remember { mutableStateOf(0) }
    val directionLabels = listOf("epoch → date", "now")

    ToolScaffold(tool, container, navController) {
        OptionChipRow(options = directionLabels, selectedIndex = direction, onSelect = { direction = it; result = null })
        OptionChipRow(options = zones, selectedIndex = zoneIndex, onSelect = { zoneIndex = it })
        if (direction == 0) {
            ToolInputField(value = input, onValueChange = { input = it; result = null }, placeholderText = "e.g.  1735689600")
            RunButton(stringResource(R.string.action_run)) {
                val r = conv.fromEpoch(input, zones[zoneIndex])
                result = r
                val out = when (r) {
                    is TimestampConverterTool.TsResult.Success -> r.text
                    is TimestampConverterTool.TsResult.Failure -> "error: ${r.message}"
                }
                scope.launch { container.toolRepository.logRun(tool.id, input.take(120), out.take(120)) }
            }
        } else {
            RunButton("now") {
                input = conv.now().toString()
                result = conv.fromEpoch(input, zones[zoneIndex])
            }
        }
        when (val r = result) {
            is TimestampConverterTool.TsResult.Success -> {
                ResultCard(title = stringResource(R.string.label_result), content = r.text)
                r.details.forEach { (k, v) ->
                    Row(horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(k, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(v, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                ResultActionsRow(result = r.text)
            }
            is TimestampConverterTool.TsResult.Failure -> ErrorBanner(r.message)
            null -> {}
        }
    }
}

// ---------------- Unit Converter ----------------

@Composable
fun UnitConverterToolScreen(container: AppContainer, navController: NavHostController) {
    val tool = com.drs.nexus.core.tools.ToolRegistry.byId("unit_converter")!!
    val scope = rememberCoroutineScope()
    val conv = remember { UnitConverterTool() }
    val categories = UnitConverterTool.Category.entries
    var catIndex by remember { mutableStateOf(0) }
    val category = categories[catIndex]
    val units = UnitConverterTool.Unit.entries.filter { it.category == category }
    var fromIndex by remember(catIndex) { mutableStateOf(0) }
    var toIndex by remember(catIndex) { mutableStateOf(1.coerceAtMost(units.size - 1)) }
    var valueText by remember { mutableStateOf("1") }
    var result by remember { mutableStateOf<UnitConverterTool.ConvResult?>(null) }

    ToolScaffold(tool, container, navController) {
        OptionChipRow(
            options = categories.map { it.label },
            selectedIndex = catIndex,
            onSelect = { catIndex = it; result = null }
        )
        ToolInputField(value = valueText, onValueChange = { valueText = it; result = null }, placeholderText = "1.0", minLines = 1, maxLines = 1)
        Text("From", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OptionChipRow(options = units.map { it.label }, selectedIndex = fromIndex, onSelect = { fromIndex = it; result = null })
        Text("To", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OptionChipRow(options = units.map { it.label }, selectedIndex = toIndex, onSelect = { toIndex = it; result = null })
        RunButton(stringResource(R.string.action_run)) {
            val v = valueText.trim().toDoubleOrNull()
            if (v == null) {
                result = UnitConverterTool.ConvResult.Failure("Not a valid number")
            } else {
                result = conv.convert(v, units[fromIndex], units[toIndex])
                val out = (result as? UnitConverterTool.ConvResult.Success)?.formatted
                scope.launch { container.toolRepository.logRun(tool.id, "$v ${units[fromIndex].label}", out?.take(120) ?: "error") }
            }
        }
        when (val r = result) {
            is UnitConverterTool.ConvResult.Success -> {
                ResultCard(
                    title = "${units[fromIndex].label} → ${units[toIndex].label}",
                    content = "${r.formatted} ${units[toIndex].label}"
                )
                ResultActionsRow(result = "${r.formatted} ${units[toIndex].label}")
            }
            is UnitConverterTool.ConvResult.Failure -> ErrorBanner(r.message)
            null -> {}
        }
    }
}

// ---------------- UUID Generator ----------------

@Composable
fun UuidToolScreen(container: AppContainer, navController: NavHostController) {
    val tool = com.drs.nexus.core.tools.ToolRegistry.byId("uuid")!!
    val scope = rememberCoroutineScope()
    val uuidTool = remember { UuidTool() }
    var count by remember { mutableFloatStateOf(1f) }
    var uppercase by remember { mutableStateOf(false) }
    var hyphens by remember { mutableStateOf(true) }
    var output by remember { mutableStateOf<List<String>?>(null) }

    ToolScaffold(tool, container, navController) {
        Text("count: ${count.toInt()}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(
            value = count,
            onValueChange = { count = it },
            valueRange = 1f..20f,
            steps = 18,
            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            com.drs.nexus.app.ui.components.ActionButton(
                icon = Icons.Filled.Refresh,
                label = if (uppercase) "UPPERCASE" else "lowercase",
                onClick = { uppercase = !uppercase; output = null },
                tint = MaterialTheme.colorScheme.primary
            )
            com.drs.nexus.app.ui.components.ActionButton(
                icon = Icons.Filled.Refresh,
                label = if (hyphens) "with hyphens" else "no hyphens",
                onClick = { hyphens = !hyphens; output = null },
                tint = MaterialTheme.colorScheme.primary
            )
        }
        RunButton(stringResource(R.string.action_generate)) {
            output = uuidTool.generate(
                count = count.toInt(),
                options = UuidTool.UuidOptions(uppercase = uppercase, withHyphens = hyphens)
            )
            scope.launch {
                container.toolRepository.logRun(tool.id, "count=${count.toInt()}", output?.firstOrNull()?.take(120) ?: "")
            }
        }
        output?.let { list ->
            ResultCard(title = stringResource(R.string.label_result), content = list.joinToString("\n"))
            ResultActionsRow(result = list.joinToString("\n"))
        }
    }
}

// ---------------- Password Generator ----------------

@Composable
fun PasswordToolScreen(container: AppContainer, navController: NavHostController) {
    val tool = com.drs.nexus.core.tools.ToolRegistry.byId("password")!!
    val scope = rememberCoroutineScope()
    val gen = remember { PasswordGenerator() }
    var length by remember { mutableFloatStateOf(16f) }
    var upper by remember { mutableStateOf(true) }
    var lower by remember { mutableStateOf(true) }
    var digits by remember { mutableStateOf(true) }
    var symbols by remember { mutableStateOf(true) }
    var noAmbiguous by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf<String?>(null) }
    val strengthLabels = listOf(
        stringResource(R.string.password_strength_weak),
        stringResource(R.string.password_strength_fair),
        stringResource(R.string.password_strength_strong),
        stringResource(R.string.password_strength_very_strong)
    )

    val options = PasswordGenerator.Options(
        length = length.toInt(),
        uppercase = upper, lowercase = lower, digits = digits, symbols = symbols, excludeAmbiguous = noAmbiguous
    )
    val bits = gen.entropyBits(options)
    val strengthIdx = when {
        bits >= 100 -> 3; bits >= 70 -> 2; bits >= 45 -> 1; else -> 0
    }

    ToolScaffold(tool, container, navController) {
        Text("length: ${length.toInt()}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(
            value = length,
            onValueChange = { length = it },
            valueRange = 6f..64f,
            steps = 57,
            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
        )
        ToggleRow("A-Z", upper, { upper = it })
        ToggleRow("a-z", lower, { lower = it })
        ToggleRow("0-9", digits, { digits = it })
        ToggleRow("!@#", symbols, { symbols = it })
        ToggleRow("exclude Il1O0", noAmbiguous, { noAmbiguous = it })
        Text(
            "${stringResource(R.string.entropy_bits)}: $bits bits — ${strengthLabels[strengthIdx]}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        RunButton(stringResource(R.string.action_generate)) {
            password = try {
                gen.generate(options)
            } catch (e: IllegalArgumentException) {
                null
            }
            password?.let { pw ->
                scope.launch { container.toolRepository.logRun(tool.id, "len=${length.toInt()}", pw.take(120)) }
            }
        }
        password?.let { pw ->
            ResultCard(title = stringResource(R.string.label_result), content = pw)
            ResultActionsRow(result = pw)
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

// ---------------- Lorem Ipsum ----------------

@Composable
fun LoremToolScreen(container: AppContainer, navController: NavHostController) {
    val tool = com.drs.nexus.core.tools.ToolRegistry.byId("lorem")!!
    val scope = rememberCoroutineScope()
    val lorem = remember { LoremIpsumTool() }
    val modes = LoremIpsumTool.Mode.entries
    val modeLabels = listOf("paragraphs", "sentences", "words")
    var modeIndex by remember { mutableStateOf(0) }
    var count by remember { mutableFloatStateOf(3f) }
    var output by remember { mutableStateOf<String?>(null) }

    ToolScaffold(tool, container, navController) {
        OptionChipRow(options = modeLabels, selectedIndex = modeIndex, onSelect = { modeIndex = it; output = null })
        Text("count: ${count.toInt()}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(
            value = count,
            onValueChange = { count = it },
            valueRange = 1f..20f,
            steps = 18,
            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
        )
        RunButton(stringResource(R.string.action_generate)) {
            output = lorem.generate(modes[modeIndex], count.toInt())
            scope.launch { container.toolRepository.logRun(tool.id, "${modes[modeIndex]} x${count.toInt()}", output!!.take(120)) }
        }
        output?.let { out ->
            ResultCard(title = stringResource(R.string.label_result), content = out)
            ResultActionsRow(result = out)
        }
    }
}

// ---------------- Text Utilities ----------------

@Composable
fun TextUtilsToolScreen(container: AppContainer, navController: NavHostController) {
    val tool = com.drs.nexus.core.tools.ToolRegistry.byId("text_utils")!!
    val scope = rememberCoroutineScope()
    val textTool = remember { TextUtilitiesTool() }
    var input by remember { mutableStateOf("") }
    var output by remember { mutableStateOf<String?>(null) }
    var stats by remember { mutableStateOf<TextUtilitiesTool.TextStats?>(null) }
    val cases = TextUtilitiesTool.CaseStyle.entries
    val caseLabels = listOf("camelCase", "snake_case", "kebab-case", "PascalCase", "UPPER", "lower", "Title Case")
    var caseIndex by remember { mutableStateOf(0) }
    val actions = listOf("stats", "reverse", "trim lines", "collapse spaces", "normalize")
    var actionIndex by remember { mutableStateOf(0) }

    ToolScaffold(tool, container, navController) {
        OptionChipRow(options = actions, selectedIndex = actionIndex, onSelect = { actionIndex = it; output = null })
        if (actionIndex == 4) {
            OptionChipRow(options = caseLabels, selectedIndex = caseIndex, onSelect = { caseIndex = it })
        }
        ToolInputField(value = input, onValueChange = { input = it; output = null; stats = null })
        RunButton(stringResource(R.string.action_run)) {
            when (actionIndex) {
                0 -> stats = textTool.stats(input)
                1 -> output = textTool.reverse(input)
                2 -> output = textTool.trimLines(input)
                3 -> output = textTool.removeExtraSpaces(input)
                else -> {
                    stats = textTool.stats(input)
                    output = textTool.convertCase(input, cases[caseIndex])
                }
            }
            val out = output ?: stats?.let { "words=${it.words}" } ?: ""
            scope.launch { container.toolRepository.logRun(tool.id, actions[actionIndex], out.take(120)) }
        }
        stats?.let { s ->
            ResultCard(
                title = stringResource(R.string.stats_summary),
                content = "chars=${s.characters} · no-spaces=${s.charactersNoSpaces} · words=${s.words} · lines=${s.lines} · sentences=${s.sentences} · paragraphs=${s.paragraphs} · unique=${s.uniqueWords}"
            )
        }
        output?.let { out ->
            ResultCard(title = stringResource(R.string.label_result), content = out)
            ResultActionsRow(result = out)
        }
    }
}
