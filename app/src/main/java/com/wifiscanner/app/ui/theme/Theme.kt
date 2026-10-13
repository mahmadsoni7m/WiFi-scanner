package com.wifiscanner.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class ThemeMode { SYSTEM, LIGHT, DARK }

fun ThemeMode.isDark(systemDark: Boolean): Boolean = when (this) {
    ThemeMode.SYSTEM -> systemDark
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

/** Brand and status colors shared by both themes. */
object WsColors {
    val Blue = Color(0xFF2E7BFF)
    val Cyan = Color(0xFF00E5FF)
    val Success = Color(0xFF2EE6A6)
    val Warning = Color(0xFFFFB84D)
    val Danger = Color(0xFFFF5C7A)
    val GradientStart = Color(0xFF1B4FD8)
    val GradientEnd = Color(0xFF0A86AD)
}

private val DarkScheme = darkColorScheme(
    primary = WsColors.Blue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF123A8C),
    onPrimaryContainer = Color(0xFFD6E3FF),
    secondary = WsColors.Cyan,
    onSecondary = Color(0xFF00363D),
    secondaryContainer = Color(0xFF0B3A47),
    onSecondaryContainer = Color(0xFFB8F3FF),
    tertiary = WsColors.Success,
    background = Color(0xFF070B1A),
    onBackground = Color(0xFFE6ECFF),
    surface = Color(0xFF0F1830),
    onSurface = Color(0xFFE6ECFF),
    surfaceVariant = Color(0xFF18264F),
    onSurfaceVariant = Color(0xFFA9B6D9),
    outline = Color(0xFF3A4A7A),
    error = WsColors.Danger
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF1F5FE0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9E5FF),
    onPrimaryContainer = Color(0xFF0A2A6B),
    secondary = Color(0xFF007A92),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC8F1FA),
    onSecondaryContainer = Color(0xFF00363F),
    tertiary = Color(0xFF008A65),
    background = Color(0xFFF3F6FD),
    onBackground = Color(0xFF0B1230),
    surface = Color.White,
    onSurface = Color(0xFF0B1230),
    surfaceVariant = Color(0xFFE5EBF9),
    onSurfaceVariant = Color(0xFF47527A),
    outline = Color(0xFF9AA6CC),
    error = Color(0xFFC62848)
)

private val AppTypography = Typography(
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 12.sp, lineHeight = 17.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp)
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)

@Composable
fun WifiScannerTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = mode.isDark(isSystemInDarkTheme())
    MaterialTheme(
        colorScheme = if (dark) DarkScheme else LightScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
