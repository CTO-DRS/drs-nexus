package com.drs.nexus.app.ui.screens.tools

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.drs.nexus.app.R
import com.drs.nexus.app.data.AppContainer
import com.drs.nexus.app.ui.components.ActionButton
import com.drs.nexus.app.ui.components.ResultActionsRow
import com.drs.nexus.app.ui.components.ResultCard
import com.drs.nexus.core.tools.CurrencyConverterTool
import kotlinx.coroutines.launch

// ---------------- Offline Currency Converter ----------------

@Composable
fun CurrencyToolScreen(container: AppContainer, navController: NavHostController) {
    val tool = com.drs.nexus.core.tools.ToolRegistry.byId("currency")!!
    val scope = rememberCoroutineScope()
    val conv = remember { CurrencyConverterTool() }
    val currencies = CurrencyConverterTool.CURRENCIES
    val codes = currencies.map { it.code }
    var fromIndex by remember { mutableStateOf(codes.indexOf("USD").coerceAtLeast(0)) }
    var toIndex by remember { mutableStateOf(codes.indexOf("SAR").coerceAtLeast(0)) }
    var amount by remember { mutableStateOf("100") }
    var result by remember { mutableStateOf<CurrencyConverterTool.Result?>(null) }

    ToolScaffold(tool, container, navController) {
        Text(
            stringResource(R.string.currency_static_notice, CurrencyConverterTool.RATES_DATE),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        ToolInputField(
            value = amount,
            onValueChange = { amount = it; result = null },
            placeholderText = "100",
            minLines = 1,
            maxLines = 1
        )
        Text(
            stringResource(R.string.currency_from),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OptionChipRow(options = codes, selectedIndex = fromIndex, onSelect = { fromIndex = it; result = null })
        Row(verticalAlignment = Alignment.CenterVertically) {
            ActionButton(
                icon = Icons.Filled.SwapVert,
                label = stringResource(R.string.currency_swap),
                onClick = {
                    val f = fromIndex; fromIndex = toIndex; toIndex = f; result = null
                }
            )
        }
        Text(
            stringResource(R.string.currency_to),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OptionChipRow(options = codes, selectedIndex = toIndex, onSelect = { toIndex = it; result = null })
        RunButton(stringResource(R.string.action_convert)) {
            val a = amount.trim().toDoubleOrNull()
            result = if (a == null) {
                CurrencyConverterTool.Result.Failure("Please enter a valid amount")
            } else {
                conv.convert(a, codes[fromIndex], codes[toIndex])
            }
            val out = when (val r = result) {
                is CurrencyConverterTool.Result.Success -> conv.formatResult(r.conversion)
                is CurrencyConverterTool.Result.Failure -> "error: ${r.message}"
                null -> ""
            }
            scope.launch { container.toolRepository.logRun(tool.id, "$a ${codes[fromIndex]}→${codes[toIndex]}", out.take(120)) }
        }
        when (val r = result) {
            is CurrencyConverterTool.Result.Success -> {
                ResultCard(
                    title = "${r.conversion.from} → ${r.conversion.to}",
                    content = conv.formatResult(r.conversion)
                )
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                    Text(
                        stringResource(
                            R.string.currency_rate_line,
                            r.conversion.unitRate.toString(),
                            r.conversion.from,
                            r.conversion.to
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                ResultActionsRow(result = conv.formatResult(r.conversion))
            }
            is CurrencyConverterTool.Result.Failure -> ErrorBanner(r.message)
            null -> {}
        }
        Column(Modifier.padding(top = 4.dp)) {
            currencies.forEach { c ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                ) {
                    Text("${c.code} — ${c.nameEn}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(c.ratePerUsd.toString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}
