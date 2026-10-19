package com.drs.nexus.app.ui.screens.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.drs.nexus.app.R
import com.drs.nexus.app.data.AppContainer
import com.drs.nexus.app.ui.components.ResultActionsRow
import com.drs.nexus.app.ui.components.ResultCard
import com.drs.nexus.app.ui.theme.NexusShapes
import com.drs.nexus.core.tools.AgeCalculatorTool
import com.drs.nexus.core.tools.RandomGeneratorTool
import com.drs.nexus.core.tools.UrlParserTool
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** Compact labeled input shared by the v5.2 tool screens. */
@Composable
private fun ToolTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    numeric: Boolean = false,
    placeholder: String = ""
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = NexusShapes.input,
        label = { Text(label) },
        placeholder = if (placeholder.isEmpty()) null else {
            { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        },
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = if (numeric) KeyboardType.Decimal else KeyboardType.Text
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface
        ),
        textStyle = MaterialTheme.typography.bodyMedium
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value.ifBlank { "—" },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

private fun parseDateOr(text: String): LocalDate? = try {
    LocalDate.parse(text.trim(), DateTimeFormatter.ISO_LOCAL_DATE)
} catch (e: Exception) {
    null
}

// ---------------- Age Calculator ----------------

@Composable
fun AgeCalcToolScreen(container: AppContainer, navController: NavHostController) {
    val tool = com.drs.nexus.core.tools.ToolRegistry.byId("age_calc")!!
    val scope = rememberCoroutineScope()
    val calc = remember { AgeCalculatorTool() }
    var birth by remember { mutableStateOf("2000-01-15") }
    var asOf by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<AgeCalculatorTool.Result?>(null) }

    ToolScaffold(tool, container, navController) {
        Text(
            stringResource(R.string.age_format_hint),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        ToolTextField(
            value = birth,
            onValueChange = { birth = it; result = null },
            label = stringResource(R.string.age_birth_date),
            placeholder = "1995-06-21"
        )
        ToolTextField(
            value = asOf,
            onValueChange = { asOf = it; result = null },
            label = stringResource(R.string.age_asof_date),
            placeholder = stringResource(R.string.age_asof_today),
            numeric = false
        )
        RunButton(stringResource(R.string.action_calculate)) {
            val b = parseDateOr(birth)
            result = if (b == null) {
                AgeCalculatorTool.Result.Failure("Invalid birth date — expected YYYY-MM-DD")
            } else {
                val t = asOf.trim()
                if (t.isEmpty()) calc.compute(b, LocalDate.now()) else {
                    val a = parseDateOr(t)
                    if (a == null) {
                        AgeCalculatorTool.Result.Failure("Invalid reference date — expected YYYY-MM-DD")
                    } else {
                        calc.compute(b, a)
                    }
                }
            }
            val out = when (val r = result) {
                is AgeCalculatorTool.Result.Success -> calc.formatResult(r.age)
                is AgeCalculatorTool.Result.Failure -> "error: ${r.message}"
                null -> ""
            }
            scope.launch { container.toolRepository.logRun(tool.id, birth.trim(), out.take(120)) }
        }
        when (val r = result) {
            is AgeCalculatorTool.Result.Success -> {
                val a = r.age
                ResultCard(
                    title = stringResource(R.string.age_exact),
                    content = stringResource(R.string.age_ymd, a.years, a.months, a.days)
                )
                Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp)) {
                    DetailRow(stringResource(R.string.age_total_months), a.totalMonths.toString())
                    DetailRow(stringResource(R.string.age_total_weeks), a.totalWeeks.toString())
                    DetailRow(stringResource(R.string.age_total_days), a.totalDays.toString())
                    DetailRow(
                        stringResource(R.string.age_birth_weekday),
                        a.birthDayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
                    )
                    if (a.isBirthdayToday) {
                        Text(
                            stringResource(R.string.age_birthday_today),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        DetailRow(
                            stringResource(R.string.age_next_birthday),
                            stringResource(R.string.age_in_days, a.nextBirthdayInDays, a.nextBirthdayDate.toString())
                        )
                    }
                }
                ResultActionsRow(result = calc.formatResult(a), title = stringResource(R.string.tool_age_name))
            }
            is AgeCalculatorTool.Result.Failure -> ErrorBanner(r.message)
            null -> {}
        }
    }
}

// ---------------- URL Parser ----------------

@Composable
fun UrlParserToolScreen(container: AppContainer, navController: NavHostController) {
    val tool = com.drs.nexus.core.tools.ToolRegistry.byId("url_parser")!!
    val scope = rememberCoroutineScope()
    val parser = remember { UrlParserTool() }
    var input by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<UrlParserTool.Result?>(null) }

    ToolScaffold(tool, container, navController) {
        ToolInputField(
            value = input,
            onValueChange = { input = it; result = null },
            minLines = 2,
            maxLines = 4,
            placeholderText = "https://example.com:8443/api/v2?q=nexus&lang=ar#top"
        )
        RunButton(stringResource(R.string.action_parse)) {
            result = parser.parse(input)
            val out = when (val r = result) {
                is UrlParserTool.Result.Success -> parser.formatResult(r.parsed)
                is UrlParserTool.Result.Failure -> "error: ${r.message}"
                null -> ""
            }
            scope.launch { container.toolRepository.logRun(tool.id, input.trim().take(80), out.take(120)) }
        }
        when (val r = result) {
            is UrlParserTool.Result.Success -> {
                val p = r.parsed
                ResultCard(title = stringResource(R.string.urlparser_host_line), content = parser.formatResult(p))
                Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp)) {
                    DetailRow(stringResource(R.string.urlparser_scheme), p.scheme)
                    DetailRow(stringResource(R.string.urlparser_host), p.host)
                    DetailRow(
                        stringResource(R.string.urlparser_port),
                        "${p.port}" + if (p.isDefaultPort) " (${stringResource(R.string.urlparser_default)})" else ""
                    )
                    DetailRow(stringResource(R.string.urlparser_path), p.path.ifBlank { "/" })
                    if (p.pathSegments.isNotEmpty()) {
                        DetailRow(stringResource(R.string.urlparser_segments), p.pathSegments.joinToString(" › "))
                    }
                    DetailRow(stringResource(R.string.urlparser_query), p.query.ifBlank { "—" })
                    if (p.fragment.isNotEmpty()) DetailRow(stringResource(R.string.urlparser_fragment), p.fragment)
                    if (p.userInfo.isNotEmpty()) DetailRow(stringResource(R.string.urlparser_userinfo), p.userInfo)
                    if (p.params.isNotEmpty()) {
                        HorizontalDivider(Modifier.padding(vertical = 6.dp))
                        Text(
                            stringResource(R.string.urlparser_params, p.params.size),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        p.params.forEach { param ->
                            DetailRow(param.key, param.value)
                        }
                    }
                }
                Text(
                    stringResource(R.string.urlparser_normalized, parser.normalize(p)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                ResultActionsRow(result = parser.formatResult(p), title = stringResource(R.string.tool_urlparser_name))
            }
            is UrlParserTool.Result.Failure -> ErrorBanner(r.message)
            null -> {}
        }
    }
}

// ---------------- Random Generator ----------------

@Composable
fun RandomGenToolScreen(container: AppContainer, navController: NavHostController) {
    val tool = com.drs.nexus.core.tools.ToolRegistry.byId("random_gen")!!
    val scope = rememberCoroutineScope()
    val gen = remember { RandomGeneratorTool() }
    val modes = listOf(
        stringResource(R.string.random_mode_numbers),
        stringResource(R.string.random_mode_dice),
        stringResource(R.string.random_mode_coin)
    )
    val uniqueOptions = listOf(
        stringResource(R.string.random_unique_off),
        stringResource(R.string.random_unique_on)
    )
    var mode by remember { mutableStateOf(0) }
    var unique by remember { mutableStateOf(0) }
    var min by remember { mutableStateOf("1") }
    var max by remember { mutableStateOf("100") }
    var count by remember { mutableStateOf("5") }
    var result by remember { mutableStateOf<RandomGeneratorTool.Result?>(null) }

    ToolScaffold(tool, container, navController) {
        OptionChipRow(options = modes, selectedIndex = mode, onSelect = { mode = it; result = null })
        if (mode == 0) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                androidx.compose.foundation.layout.Box(Modifier.weight(1f)) {
                    ToolTextField(
                        value = min,
                        onValueChange = { min = it; result = null },
                        label = stringResource(R.string.random_min),
                        numeric = true
                    )
                }
                androidx.compose.foundation.layout.Box(Modifier.weight(1f)) {
                    ToolTextField(
                        value = max,
                        onValueChange = { max = it; result = null },
                        label = stringResource(R.string.random_max),
                        numeric = true
                    )
                }
            }
            OptionChipRow(options = uniqueOptions, selectedIndex = unique, onSelect = { unique = it })
        }
        if (mode != 2) {
            ToolTextField(
                value = count,
                onValueChange = { count = it; result = null },
                label = stringResource(R.string.random_count),
                numeric = true
            )
        }
        RunButton(stringResource(R.string.action_generate)) {
            val req = when (mode) {
                0 -> RandomGeneratorTool.Request(
                    mode = RandomGeneratorTool.Mode.INTEGERS,
                    min = min.trim().toLongOrNull() ?: Long.MIN_VALUE / 2,
                    max = max.trim().toLongOrNull() ?: Long.MAX_VALUE / 2,
                    count = count.trim().toIntOrNull() ?: 1,
                    unique = unique == 1
                )
                1 -> RandomGeneratorTool.Request(
                    mode = RandomGeneratorTool.Mode.DICE,
                    count = count.trim().toIntOrNull() ?: 1
                )
                else -> RandomGeneratorTool.Request(
                    mode = RandomGeneratorTool.Mode.COIN,
                    count = 1
                )
            }
            result = gen.generate(req)
            val out = when (val r = result) {
                is RandomGeneratorTool.Result.Success -> gen.formatResult(r)
                is RandomGeneratorTool.Result.Failure -> "error: ${r.message}"
                null -> ""
            }
            scope.launch { container.toolRepository.logRun(tool.id, modes[mode], out.take(120)) }
        }
        when (val r = result) {
            is RandomGeneratorTool.Result.Success -> {
                ResultCard(title = modes[mode], content = gen.formatResult(r))
                ResultActionsRow(result = gen.formatResult(r), title = stringResource(R.string.tool_random_name))
            }
            is RandomGeneratorTool.Result.Failure -> ErrorBanner(r.message)
            null -> {}
        }
    }
}
