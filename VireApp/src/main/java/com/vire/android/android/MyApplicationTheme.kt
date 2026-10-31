package com.vire.android.android

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// VIRE — Midnight Circuit Compose Color Palette
val CircuitLime = Color(0xFFCEF23A)
val CircuitLimeLight = Color(0xFFE0FA62)
val CircuitLimeDark = Color(0xFFB2D428)
val CircuitLimeMuted = Color(0xFF2A3614)
val CircuitBlack = Color(0xFF0C0E10)

val CircuitSecondary = Color(0xFF2DD4BF)
val CircuitSecondaryDark = Color(0xFF14B8A6)
val CircuitSecondaryMuted = Color(0xFF132E2B)

val CircuitBackground = Color(0xFF0C0E10)
val CircuitCard = Color(0xFF16191D)
val CircuitSurface = Color(0xFF1E2329)
val CircuitSurfaceBorder = Color(0xFF2A3038)

val CircuitTextPrimary = Color(0xFFF3F4F6)
val CircuitTextSecondary = Color(0xFF9CA3AF)
val CircuitTextMuted = Color(0xFF6B7280)

val CircuitError = Color(0xFFEF4444)

private val MidnightCircuitColorScheme = darkColorScheme(
    primary = CircuitLime,
    onPrimary = CircuitBlack,
    primaryContainer = CircuitLimeMuted,
    onPrimaryContainer = CircuitLimeLight,
    secondary = CircuitSecondary,
    onSecondary = CircuitBlack,
    secondaryContainer = CircuitSecondaryMuted,
    onSecondaryContainer = CircuitSecondary,
    background = CircuitBackground,
    onBackground = CircuitTextPrimary,
    surface = CircuitCard,
    onSurface = CircuitTextPrimary,
    surfaceVariant = CircuitSurface,
    onSurfaceVariant = CircuitTextSecondary,
    outline = CircuitSurfaceBorder,
    error = CircuitError,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // VIRE's Midnight Circuit theme is intentionally dark across all system modes
    val colors = MidnightCircuitColorScheme

    val typography = Typography(
        titleLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = CircuitTextPrimary
        ),
        bodyLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            color = CircuitTextPrimary
        ),
        bodyMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            color = CircuitTextSecondary
        ),
        labelLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = CircuitBlack
        )
    )

    val shapes = Shapes(
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(12.dp),
        large = RoundedCornerShape(16.dp)
    )

    MaterialTheme(
        colorScheme = colors,
        typography = typography,
        shapes = shapes,
        content = content
    )
}
