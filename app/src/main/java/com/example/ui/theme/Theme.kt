package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = BluePrimary,
    onPrimary = Color.White,
    primaryContainer = BlueLight,
    onPrimaryContainer = BlueDark,
    secondary = BlueSecondary,
    onSecondary = Color.White,
    secondaryContainer = BlueLight,
    onSecondaryContainer = BlueDark,
    tertiary = OrangeWarning,
    onTertiary = Color.White,
    tertiaryContainer = OrangeLight,
    onTertiaryContainer = OrangeWarning,
    background = BackgroundLight,
    onBackground = TextPrimary,
    surface = SurfaceWhite,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondary,
    error = RedAccent,
    onError = Color.White,
    outline = BorderLight,
    outlineVariant = BorderLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = AppTypography,
        content = content
    )
}
