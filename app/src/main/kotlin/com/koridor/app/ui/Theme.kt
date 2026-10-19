package com.koridor.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Colors = darkColorScheme(
    primary = Color(0xFFF2C88B),
    onPrimary = Color(0xFF3B2414),
    secondary = Color(0xFFC98B55),
    onSecondary = Color(0xFF241509),
    background = Color(0xFF1B1410),
    onBackground = Color(0xFFF3E7DA),
    surface = Color(0xFF2A1F18),
    onSurface = Color(0xFFF3E7DA),
    surfaceVariant = Color(0xFF3A2C22),
    onSurfaceVariant = Color(0xFFD9C6B3),
    outline = Color(0xFF8C7562),
)

@Composable
fun KoridorTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, content = content)
}
