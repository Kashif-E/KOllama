package com.kashif.kollama.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


internal val LocalThemeIsDark = compositionLocalOf { mutableStateOf(true) }

val DarkColors = darkColorScheme(
    primary = Color(0xFF66CFFF),
    surface = Color(0xFF1A1F2E),
    surfaceVariant = Color(0xFF252A3A),
    onSurface = Color(0xFFE3E8ED),
    onSurfaceVariant = Color(0xFF9BA5B0),
    primaryContainer = Color(0xFF1E3A5F),
    onPrimaryContainer = Color(0xFF89DAFF),
    background = Color(0xFF0F111A),
    onBackground = Color(0xFFE3E8ED),
    secondary = Color(0xFF7F66FF),
    error = Color(0xFFFF6B6B),
    tertiary = Color(0xFF66FFB3),
    tertiaryContainer = Color(0xFF1E5F3A),
    onTertiaryContainer = Color(0xFF89FFB3)
)

val LightColors = lightColorScheme(
    primary = Color(0xFF0086CC),
    surface = Color(0xFFF8FAFF),
    surfaceVariant = Color(0xFFF0F4F8),
    onSurface = Color(0xFF1A1F2E),
    onSurfaceVariant = Color(0xFF445668),
    primaryContainer = Color(0xFFE5F6FF),
    onPrimaryContainer = Color(0xFF0086CC),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1A1F2E),
    secondary = Color(0xFF5C48CC),
    error = Color(0xFFD32F2F),
    tertiary = Color(0xFF00CC77),
    tertiaryContainer = Color(0xFFE5FFF2),
    onTertiaryContainer = Color(0xFF00CC77)
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val isDark = remember { mutableStateOf(darkTheme) }
    CompositionLocalProvider(
        LocalThemeIsDark provides isDark
    ) {
        val colorScheme = if (isDark.value) DarkColors else LightColors
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}


private val Typography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    )
)

private val Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)


