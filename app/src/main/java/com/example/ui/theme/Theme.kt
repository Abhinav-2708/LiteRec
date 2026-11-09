package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LiteRecColorScheme = darkColorScheme(
    primary = OneUIRecordRed,
    onPrimary = Color.White,
    primaryContainer = OneUIRecordRedDark,
    onPrimaryContainer = Color.White,
    secondary = OneUIIdleGreen,
    onSecondary = Color.White,
    secondaryContainer = OneUISurfaceVariant,
    onSecondaryContainer = OneUITextPrimary,
    tertiary = OneUIAccentBlue,
    background = OneUIBackground,
    onBackground = OneUITextPrimary,
    surface = OneUISurface,
    onSurface = OneUITextPrimary,
    surfaceVariant = OneUISurfaceVariant,
    onSurfaceVariant = OneUITextSecondary,
    outline = OneUIBorder,
    outlineVariant = OneUIBorder
)

@Composable
fun LiteRecTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LiteRecColorScheme,
        typography = Typography,
        content = content
    )
}
