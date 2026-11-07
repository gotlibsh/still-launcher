package com.gotlibsh.still.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object StillColors {
    val background = Color.Black
    val text = Color(0xFFEDEDED)
    val dim = Color(0xFF8C8C8C)
    val faint = Color(0xFF4A4A4A)
}

@Composable
fun StillTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = StillColors.text,
            onPrimary = Color.Black,
            background = StillColors.background,
            onBackground = StillColors.text,
            surface = Color(0xFF121212),
            onSurface = StillColors.text,
            surfaceContainer = Color(0xFF121212),
        ),
        content = content,
    )
}
