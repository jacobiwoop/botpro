package com.example.botpro.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TelegramDarkColorScheme = darkColorScheme(
    primary = TelegramColors.Accent,
    onPrimary = Color.White,
    primaryContainer = TelegramColors.Header,
    onPrimaryContainer = Color.White,
    secondary = TelegramColors.Fab,
    onSecondary = Color.White,
    background = TelegramColors.Base,
    onBackground = TelegramColors.TextPrimary,
    surface = TelegramColors.Surface,
    onSurface = TelegramColors.TextPrimary,
    surfaceVariant = TelegramColors.Border,
    onSurfaceVariant = TelegramColors.TextMuted
)

@Composable
fun BotProTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TelegramDarkColorScheme,
        typography = Typography,
        content = content
    )
}
