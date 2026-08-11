package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SkyBlueColorScheme = lightColorScheme(
    primary = OceanBlueAccent,
    onPrimary = Color.White,
    primaryContainer = SkyBlueContainer,
    onPrimaryContainer = DeepSlateNavy,
    secondary = SuccessGreen,
    onSecondary = Color.White,
    secondaryContainer = SuccessGreenBg,
    onSecondaryContainer = SuccessGreen,
    tertiary = WarningAmber,
    onTertiary = Color.White,
    background = LightSurfaceBg,
    onBackground = DeepSlateNavy,
    surface = CleanPureWhite,
    onSurface = DeepSlateNavy,
    surfaceVariant = SkyBlueContainer,
    onSurfaceVariant = SlateMutedText,
    outline = SkyBorderColor,
    outlineVariant = Color(0xFFE2E8F0)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    // Defaulting to the new clean Light Sky Blue aesthetic
    MaterialTheme(
        colorScheme = SkyBlueColorScheme,
        typography = Typography,
        content = content
    )
}
