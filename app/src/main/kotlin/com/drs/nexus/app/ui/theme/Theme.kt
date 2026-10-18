package com.drs.nexus.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---- iOS-inspired palette (light) ----
object NexusPalette {
    val Blue = Color(0xFF0A84FF)
    val Indigo = Color(0xFF5E5CE6)
    val Purple = Color(0xFFBF5AF2)
    val Green = Color(0xFF30D158)
    val Orange = Color(0xFFFF9F0A)
    val Red = Color(0xFFFF453A)
    val Teal = Color(0xFF64D2FF)
    val Pink = Color(0xFFFF2D92)
    val Mint = Color(0xFF00C7BE)

    val BgLight = Color(0xFFF2F2F7)
    val CardLight = Color.White
    val TextPrimaryLight = Color(0xFF0A0A0F)
    val TextSecondaryLight = Color(0xFF6E6E78)
    val SeparatorLight = Color(0xFFE5E5EA)

    val BgDark = Color(0xFF000000)
    val CardDark = Color(0xFF1C1C1E)
    val TextPrimaryDark = Color(0xFFF5F5F7)
    val TextSecondaryDark = Color(0xFF98989F)
    val SeparatorDark = Color(0xFF2C2C2E)
}

data class NexusColorScheme(
    val background: Color,
    val card: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val separator: Color,
    val accent: Color,
    val accentSoft: Color,
    val success: Color,
    val warning: Color,
    val danger: Color
)

@Composable
fun nexusColorScheme(dark: Boolean): NexusColorScheme =
    if (dark) NexusColorScheme(
        background = NexusPalette.BgDark,
        card = NexusPalette.CardDark,
        textPrimary = NexusPalette.TextPrimaryDark,
        textSecondary = NexusPalette.TextSecondaryDark,
        separator = NexusPalette.SeparatorDark,
        accent = NexusPalette.Blue,
        accentSoft = NexusPalette.Blue.copy(alpha = 0.16f),
        success = NexusPalette.Green,
        warning = NexusPalette.Orange,
        danger = NexusPalette.Red
    ) else NexusColorScheme(
        background = NexusPalette.BgLight,
        card = NexusPalette.CardLight,
        textPrimary = NexusPalette.TextPrimaryLight,
        textSecondary = NexusPalette.TextSecondaryLight,
        separator = NexusPalette.SeparatorLight,
        accent = NexusPalette.Blue,
        accentSoft = NexusPalette.Blue.copy(alpha = 0.12f),
        success = NexusPalette.Green,
        warning = NexusPalette.Orange,
        danger = NexusPalette.Red
    )

/** Category → icon tint mapping, consistent across the app. */
fun categoryColor(category: String, scheme: NexusColorScheme): Color = when (category) {
    "developer" -> scheme.accent
    "converters" -> NexusPalette.Indigo
    "generators" -> NexusPalette.Purple
    "calculators" -> NexusPalette.Mint
    "text" -> NexusPalette.Teal
    else -> scheme.accent
}

private val IosTypography = Typography(
    displaySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 41.sp, letterSpacing = 0.4.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp, letterSpacing = 0.1.sp),
    titleSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 24.sp, letterSpacing = 0.1.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.1.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 16.sp, letterSpacing = 0.3.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.4.sp)
)

object NexusShapes {
    val card = RoundedCornerShape(18.dp)
    val cardSmall = RoundedCornerShape(12.dp)
    val button = RoundedCornerShape(14.dp)
    val input = RoundedCornerShape(14.dp)
    val sheet = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
}

@Composable
fun NexusTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val scheme = nexusColorScheme(darkTheme)
    // We keep MaterialTheme active for M3 components but drive most visuals
    // through our NexusColorScheme via CompositionLocal-like pattern.
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            primary = scheme.accent,
            onPrimary = Color.White,
            background = scheme.background,
            onBackground = scheme.textPrimary,
            surface = scheme.card,
            onSurface = scheme.textPrimary,
            surfaceVariant = scheme.card,
            onSurfaceVariant = scheme.textSecondary,
            outline = scheme.separator
        ),
        typography = IosTypography,
        content = content
    )
}
