package com.drs.nexus.app.data

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Renders a tool result as a branded PNG card (1080px wide, height measured
 * from the wrapped text) and hands it to the Android share sheet through a
 * FileProvider URI — no storage permission involved.
 *
 * Text is laid out with StaticLayout, so Arabic and other RTL scripts shape
 * and wrap correctly, and long results are truncated gracefully.
 */
object ShareImage {

    private const val CARD_WIDTH = 1080
    private const val PADDING = 64
    private const val CORNER_RADIUS = 48f
    private const val MAX_CONTENT_LINES = 24

    // Brand palette (matches the in-app Mint theme)
    private const val BG_DARK = 0xFF111827.toInt()
    private const val CARD_BG = 0xFF1B2437.toInt()
    private const val ACCENT = 0xFF00C7BE.toInt()
    private const val TEXT_PRIMARY = 0xFFF8FAFC.toInt()
    private const val TEXT_SECONDARY = 0xFF94A3B8.toInt()

    private val stampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

    /** Renders [title]/[content] into a PNG file inside cacheDir/shared. */
    fun renderCard(context: Context, title: String, content: String): File {
        val contentPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = TEXT_PRIMARY
            textSize = 46f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ACCENT
            textSize = 58f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val brandPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = TEXT_SECONDARY
            textSize = 38f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = TEXT_SECONDARY
            textSize = 34f
        }

        val innerWidth = (CARD_WIDTH - 2 * PADDING).toFloat()
        val contentLayout = StaticLayout.Builder
            .obtain(content.take(4000), 0, content.take(4000).length, contentPaint, innerWidth.toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(10f, 1f)
            .setEllipsize(TextUtils.TruncateAt.END)
            .setMaxLines(MAX_CONTENT_LINES)
            .build()
        val titleLayout = StaticLayout.Builder
            .obtain(title, 0, title.length, titlePaint, innerWidth.toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setMaxLines(3)
            .setEllipsize(TextUtils.TruncateAt.END)
            .build()

        val brandText = "DRS Nexus"
        val stampText = stampFormat.format(Date())
        val footerText = "$stampText  •  $brandText"

        val brandHeight = 40
        val gapAfterBrand = 24
        val titleHeight = titleLayout.height
        val dividerGap = 28
        val contentHeight = contentLayout.height
        val footerHeight = 44
        val cardHeight = PADDING + brandHeight + gapAfterBrand + titleHeight + dividerGap +
            contentHeight + PADDING + footerHeight + PADDING / 2

        val bitmap = Bitmap.createBitmap(CARD_WIDTH, cardHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Outer rounded background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = BG_DARK }
        canvas.drawRoundRect(
            RectF(0f, 0f, CARD_WIDTH.toFloat(), cardHeight.toFloat()),
            CORNER_RADIUS, CORNER_RADIUS, bgPaint
        )
        // Accent strip on the top edge
        val stripPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ACCENT }
        canvas.drawRoundRect(
            RectF(0f, 0f, CARD_WIDTH.toFloat(), 14f),
            8f, 8f, stripPaint
        )

        var y = PADDING.toFloat()

        // Brand row: small dot + name
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ACCENT }
        canvas.drawCircle(PADDING + 12f, y + 20f, 12f, dotPaint)
        canvas.drawText(brandText, PADDING + 44f, y + 34f, brandPaint)
        y += brandHeight + gapAfterBrand

        // Title
        canvas.withTranslation(PADDING.toFloat(), y) { titleLayout.draw(this) }
        y += titleHeight + dividerGap

        // Divider
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF2A3650.toInt()
            strokeWidth = 3f
        }
        canvas.drawLine(PADDING.toFloat(), y, (CARD_WIDTH - PADDING).toFloat(), y, dividerPaint)
        y += 28f

        // Content
        canvas.withTranslation(PADDING.toFloat(), y) { contentLayout.draw(this) }
        y += contentHeight + PADDING

        // Footer
        canvas.drawText(footerText, PADDING.toFloat(), y + footerHeight * 0.6f, footerPaint)

        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, "drs-nexus-result-${System.currentTimeMillis()}.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        return file
    }

    /** Renders and opens the Android share sheet for the PNG card. */
    fun shareResultCard(context: Context, title: String, content: String) {
        val trimmedContent = content.trim()
        if (trimmedContent.isEmpty()) return
        runCatching {
            val file = renderCard(context, title.ifBlank { "DRS Nexus" }, trimmedContent)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "$title\n\n$trimmedContent")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(
                Intent.createChooser(intent, null).apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            )
        }
    }

    private inline fun Canvas.withTranslation(x: Float, y: Float, block: Canvas.() -> Unit) {
        val checkpoint = save()
        translate(x, y)
        try {
            block()
        } finally {
            restoreToCount(checkpoint)
        }
    }
}
