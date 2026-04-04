package com.zephyr.boreal.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = BorealColors.Blue400,
    secondary = BorealColors.Magenta700,
    tertiary = BorealColors.Green600,
    background = BorealColors.Background,
    surface = BorealColors.Neutral,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
  )

@Composable
fun BorealTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = DarkColorScheme,
    typography = BorealTypography,
    content = content,
  )
}
