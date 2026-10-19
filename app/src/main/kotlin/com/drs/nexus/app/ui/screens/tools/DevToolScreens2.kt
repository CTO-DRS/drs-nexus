package com.drs.nexus.app.ui.screens.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.drs.nexus.app.R
import com.drs.nexus.app.data.AppContainer
import com.drs.nexus.app.ui.components.ResultActionsRow
import com.drs.nexus.app.ui.components.ResultCard
import com.drs.nexus.app.ui.theme.NexusShapes
import com.drs.nexus.core.tools.BaseConverterTool
import com.drs.nexus.core.tools.ColorConverterTool
import com.drs.nexus.core.tools.JwtDecoderTool
import kotlinx.coroutines.launch

// ---------------- JWT Decoder ----------------

@Composable
fun JwtToolScreen(container: AppContainer, navController: NavHostController) {
    val tool = com.drs.nexus.core.tools.ToolRegistry.byId("jwt_decoder")!!
    val scope = rememberCoroutineScope()
    val decoder = remember { JwtDecoderTool() }
    var input by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<JwtDecoderTool.JwtResult?>(null) }

    ToolScaffold(tool, container, navController) {
        ToolInputField(
            value = input,
            onValueChange = { input = it; result = null },
            placeholderText = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyIn0.sig",
            minLines = 4,
            maxLines = 8
        )
        RunButton(stringResource(R.string.action_decode)) {
            result = decoder.decode(input)
            val out = when (val r = result) {
                is JwtDecoderTool.JwtResult.Success -> r.result.payload.prettyJson.take(120)
                is JwtDecoderTool.JwtResult.Failure -> "error: ${r.message}"
                null -> ""
            }
            scope.launch { container.toolRepository.logRun(tool.id, "jwt", out) }
        }
        when (val r = result) {
            is JwtDecoderTool.JwtResult.Success -> {
                val d = r.result
                val statusLabel = when (d.status) {
                    JwtDecoderTool.ExpiryStatus.VALID -> stringResource(R.string.jwt_valid)
                    JwtDecoderTool.ExpiryStatus.EXPIRED -> stringResource(R.string.jwt_expired)
                    JwtDecoderTool.ExpiryStatus.NO_EXPIRY -> stringResource(R.string.jwt_no_expiry)
                }
                val statusColor = when (d.status) {
                    JwtDecoderTool.ExpiryStatus.VALID -> MaterialTheme.colorScheme.primary
                    JwtDecoderTool.ExpiryStatus.EXPIRED -> Color(0xFFE5484D)
                    JwtDecoderTool.ExpiryStatus.NO_EXPIRY -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Text(
                    statusLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = statusColor,
                    fontWeight = FontWeight.SemiBold
                )
                ResultCard(title = stringResource(R.string.jwt_header), content = d.header.prettyJson)
                ResultCard(title = stringResource(R.string.jwt_payload), content = d.payload.prettyJson)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ClaimRow("alg", d.algorithm ?: "—")
                    ClaimRow("sub", d.claims.subject ?: "—")
                    ClaimRow("iss", d.claims.issuer ?: "—")
                    ClaimRow("aud", d.claims.audience ?: "—")
                    ClaimRow("iat", d.claims.issuedAtEpoch?.toString() ?: "—")
                    ClaimRow("exp", d.claims.expiresAtEpoch?.toString() ?: "—")
                }
            }
            is JwtDecoderTool.JwtResult.Failure -> ErrorBanner(r.message)
            null -> {}
        }
    }
}

@Composable
private fun ClaimRow(key: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(key, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
    }
}

// ---------------- Number Base Converter ----------------

@Composable
fun BaseConverterToolScreen(container: AppContainer, navController: NavHostController) {
    val tool = com.drs.nexus.core.tools.ToolRegistry.byId("base_converter")!!
    val scope = rememberCoroutineScope()
    val conv = remember { BaseConverterTool() }
    val bases = listOf(2, 8, 10, 16, 36)
    var inputBaseIndex by remember { mutableStateOf(3) } // hex default
    var customBase by remember { mutableFloatStateOf(36f) }
    var input by remember { mutableStateOf("FF") }
    var result by remember { mutableStateOf<BaseConverterTool.ConvResult?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    ToolScaffold(tool, container, navController) {
        Text(
            stringResource(R.string.base_input_base),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OptionChipRow(
            options = bases.map { "base $it" },
            selectedIndex = inputBaseIndex,
            onSelect = { inputBaseIndex = it; result = null; error = null }
        )
        ToolInputField(
            value = input,
            onValueChange = { input = it; result = null; error = null },
            placeholderText = if (bases[inputBaseIndex] == 16) "1A2B" else "123",
            minLines = 1,
            maxLines = 1
        )
        Text(
            stringResource(R.string.base_custom_base) + ": ${customBase.toInt()}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Slider(
            value = customBase,
            onValueChange = { customBase = it },
            valueRange = 2f..36f,
            steps = 33,
            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
        )
        RunButton(stringResource(R.string.action_convert)) {
            val p = conv.parse(input, bases[inputBaseIndex])
            when (p) {
                is BaseConverterTool.ParseResult.Failure -> {
                    error = p.message; result = null
                    scope.launch { container.toolRepository.logRun(tool.id, input.take(60), "error: ${p.message}") }
                }
                is BaseConverterTool.ParseResult.Success -> {
                    error = null
                    result = conv.convert(p.value, bases[inputBaseIndex], customBase.toInt())
                    scope.launch { container.toolRepository.logRun(tool.id, "${p.value} b${bases[inputBaseIndex]}", result!!.hex.take(120)) }
                }
            }
        }
        error?.let { ErrorBanner(it) }
        result?.let { r ->
            ResultCard(title = stringResource(R.string.base_binary), content = r.binary)
            ResultCard(title = stringResource(R.string.base_octal), content = r.octal)
            ResultCard(title = stringResource(R.string.base_decimal), content = r.decimal)
            ResultCard(title = stringResource(R.string.base_hex), content = r.hex)
            ResultCard(title = stringResource(R.string.base_custom_base) + " ${customBase.toInt()}", content = r.customBase)
            ResultActionsRow(result = "bin=${r.binary} oct=${r.octal} dec=${r.decimal} hex=${r.hex}", title = stringResource(R.string.tool_base_name))
        }
    }
}

// ---------------- Color Converter ----------------

@Composable
fun ColorConverterToolScreen(container: AppContainer, navController: NavHostController) {
    val tool = com.drs.nexus.core.tools.ToolRegistry.byId("color_converter")!!
    val scope = rememberCoroutineScope()
    val conv = remember { ColorConverterTool() }
    var input by remember { mutableStateOf("#0A84FF") }
    var result by remember { mutableStateOf<ColorConverterTool.ParseResult?>(null) }

    ToolScaffold(tool, container, navController) {
        ToolInputField(
            value = input,
            onValueChange = { input = it; result = null },
            placeholderText = "#0A84FF",
            minLines = 1,
            maxLines = 1
        )
        RunButton(stringResource(R.string.action_convert)) {
            result = conv.parse(input)
            val out = (result as? ColorConverterTool.ParseResult.Success)?.info?.hex ?: "error"
            scope.launch { container.toolRepository.logRun(tool.id, input.take(60), out) }
        }
        when (val r = result) {
            is ColorConverterTool.ParseResult.Success -> {
                val info = r.info
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp)
                        .background(Color(info.rgb.red, info.rgb.green, info.rgb.blue), NexusShapes.card),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        info.hex,
                        style = MaterialTheme.typography.titleLarge,
                        color = if (info.bestTextColor == "#000000") Color.Black else Color.White
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color(info.rgb.red, info.rgb.green, info.rgb.blue, info.rgb.alpha), CircleShape)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.color_alpha) + ": ${info.rgb.alpha}", style = MaterialTheme.typography.bodySmall)
                }
                ResultCard(title = "HEX", content = info.hex)
                ResultCard(title = "HEX + " + stringResource(R.string.color_alpha), content = info.hexLong)
                ResultCard(title = "RGB", content = info.cssRgb)
                ResultCard(title = "HSL", content = info.cssHsl)
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.color_text_on_color), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(info.bestTextColor, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                }
                ResultActionsRow(result = "${info.hex} · ${info.cssRgb} · ${info.cssHsl}", title = stringResource(R.string.tool_color_name))
            }
            is ColorConverterTool.ParseResult.Failure -> ErrorBanner(r.message)
            null -> {}
        }
    }
}
