package com.zasko.imageloads.compose

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ImageLoadsColorScheme = lightColorScheme(
    primary = Color(0xFF018786),
    onPrimary = Color.White,
    secondary = Color(0xFF03DAC5),
    onSecondary = Color.Black,
    background = Color.White,
    onBackground = Color(0xFF333333),
    surface = Color.White,
    onSurface = Color(0xFF333333),
)

@Composable
fun ImageLoadsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ImageLoadsColorScheme,
        content = content,
    )
}
