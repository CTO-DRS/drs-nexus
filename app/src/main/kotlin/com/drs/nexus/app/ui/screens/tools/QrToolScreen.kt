package com.drs.nexus.app.ui.screens.tools

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.drs.nexus.app.R
import com.drs.nexus.app.data.AppContainer
import com.drs.nexus.app.ui.components.ActionButton
import com.drs.nexus.app.ui.theme.NexusShapes
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Real QR code generator: ZXing QRCodeWriter -> BitMatrix -> Bitmap, rendered on screen
 * and exportable as PNG through the Storage Access Framework (no storage permission needed).
 */
@Composable
fun QrToolScreen(container: AppContainer, navController: NavHostController) {
    val tool = com.drs.nexus.core.tools.ToolRegistry.byId("qr_generator")!!
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var input by remember { mutableStateOf("https://github.com/CTO-DRS/drs-nexus") }
    var size by remember { mutableFloatStateOf(320f) }
    var eccIndex by remember { mutableStateOf(1) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val eccLevels = listOf("L", "M", "Q", "H")

    var pendingBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("image/png")
    ) { uri ->
        val bmp = pendingBitmap
        if (uri != null && bmp != null) {
            scope.launch(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                        out.flush()
                    } ?: throw IllegalStateException("Cannot open output stream")
                }.onSuccess {
                    launch(Dispatchers.Main) {
                        android.widget.Toast.makeText(context, context.getString(R.string.qr_saved), android.widget.Toast.LENGTH_SHORT).show()
                    }
                }.onFailure {
                    launch(Dispatchers.Main) {
                        android.widget.Toast.makeText(context, it.message ?: "Save failed", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    fun generate() {
        val text = input.trim()
        if (text.isEmpty()) {
            error = "Content is empty"
            bitmap = null
            return
        }
        scope.launch {
            val result = withContext(Dispatchers.Default) {
                runCatching {
                    val hints = mapOf(
                        EncodeHintType.ERROR_CORRECTION to when (eccIndex) {
                            0 -> ErrorCorrectionLevel.L
                            2 -> ErrorCorrectionLevel.Q
                            3 -> ErrorCorrectionLevel.H
                            else -> ErrorCorrectionLevel.M
                        },
                        EncodeHintType.MARGIN to 2
                    )
                    val matrix = QRCodeWriter().encode(
                        text,
                        com.google.zxing.BarcodeFormat.QR_CODE,
                        size.toInt(),
                        size.toInt(),
                        hints
                    )
                    val bmp = Bitmap.createBitmap(matrix.width, matrix.height, Bitmap.Config.RGB_565)
                    for (x in 0 until matrix.width) {
                        for (y in 0 until matrix.height) {
                            bmp.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                        }
                    }
                    bmp
                }
            }
            result.onSuccess { bitmap = it; error = null }
                .onFailure { bitmap = null; error = it.message ?: "Encoding failed" }
            scope.launch {
                container.toolRepository.logRun(tool.id, "size=${size.toInt()} ecc=${eccLevels[eccIndex]}", "QR ${if (result.isSuccess) "generated" else "failed"}")
            }
        }
    }

    ToolScaffold(tool, container, navController) {
        ToolInputField(
            value = input,
            onValueChange = { input = it },
            placeholderText = "text or URL",
            minLines = 3,
            maxLines = 6
        )
        Text(
            "size: ${size.toInt()} px",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        androidx.compose.material3.Slider(
            value = size,
            onValueChange = { size = it },
            valueRange = 200f..600f,
            steps = 7,
            colors = androidx.compose.material3.SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            )
        )
        OptionChipRow(options = eccLevels.map { "ECC $it" }, selectedIndex = eccIndex, onSelect = { eccIndex = it })
        RunButton(stringResource(R.string.action_generate)) { generate() }
        error?.let { ErrorBanner(it) }
        bitmap?.let { bmp ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
                    .background(Color.White, NexusShapes.card),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "QR code",
                    modifier = Modifier.size(bmp.width.dp.coerceAtMost(320.dp)),
                    contentScale = ContentScale.Fit
                )
            }
            Row {
                ActionButton(
                    icon = Icons.Filled.Save,
                    label = stringResource(R.string.qr_save_png),
                    onClick = {
                        pendingBitmap = bmp
                        saveLauncher.launch("drs-nexus-qr.png")
                    }
                )
            }
        }
    }
}
