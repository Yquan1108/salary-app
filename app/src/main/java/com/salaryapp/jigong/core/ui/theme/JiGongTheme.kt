package com.salaryapp.jigong.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salaryapp.jigong.domain.model.FontScaleLevel

private val LightColors = lightColorScheme(
    primary = Color(0xFF0B5FFF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE8FF),
    onPrimaryContainer = Color(0xFF001A4D),
    secondary = Color(0xFF0F172A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2E8F0),
    onSecondaryContainer = Color(0xFF0F172A),
    tertiary = Color(0xFF166534),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDCFCE7),
    onTertiaryContainer = Color(0xFF052E16),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF020617),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF020617),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF334155),
    outline = Color(0xFF94A3B8),
    error = Color(0xFFB91C1C),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7CB3FF),
    onPrimary = Color(0xFF001B4D),
    primaryContainer = Color(0xFF0A3D91),
    onPrimaryContainer = Color(0xFFDCE8FF),
    secondary = Color(0xFFE2E8F0),
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = Color(0xFF334155),
    onSecondaryContainer = Color(0xFFF8FAFC),
    tertiary = Color(0xFF86EFAC),
    onTertiary = Color(0xFF052E16),
    tertiaryContainer = Color(0xFF166534),
    onTertiaryContainer = Color(0xFFDCFCE7),
    background = Color(0xFF020617),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF0F172A),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF64748B),
    error = Color(0xFFFCA5A5),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2)
)

val JiGongShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(32.dp)
)

@Composable
fun JiGongTheme(
    fontScaleLevel: FontScaleLevel,
    content: @Composable () -> Unit
) {
    val colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = typographyFor(fontScaleLevel),
        shapes = JiGongShapes,
        content = content
    )
}

private fun typographyFor(fontScaleLevel: FontScaleLevel): Typography {
    val scale = fontScaleLevel.multiplier

    fun scaled(size: Int, lineHeight: Int = size + 8, weight: FontWeight = FontWeight.Bold): TextStyle {
        return TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontSize = (size * scale).sp,
            lineHeight = (lineHeight * scale).sp,
            fontWeight = weight
        )
    }

    return Typography(
        displaySmall = scaled(38, 46),
        headlineLarge = scaled(30, 38),
        headlineMedium = scaled(26, 34),
        titleLarge = scaled(24, 32),
        titleMedium = scaled(22, 30),
        titleSmall = scaled(20, 28),
        bodyLarge = scaled(20, 30),
        bodyMedium = scaled(18, 28),
        bodySmall = scaled(18, 26),
        labelLarge = scaled(20, 28),
        labelMedium = scaled(18, 26),
        labelSmall = scaled(18, 24)
    )
}

fun JiGongScreenBrush(colorScheme: ColorScheme): Brush {
    return Brush.verticalGradient(
        colors = listOf(
            colorScheme.background,
            colorScheme.surfaceVariant.copy(alpha = 0.95f),
            colorScheme.background
        )
    )
}

fun JiGongHeroBrush(colorScheme: ColorScheme): Brush {
    return Brush.linearGradient(
        colors = listOf(
            colorScheme.primaryContainer,
            colorScheme.surfaceVariant,
            colorScheme.surface
        )
    )
}

@Composable
fun cardElevation() = CardDefaults.cardElevation(defaultElevation = 5.dp)
