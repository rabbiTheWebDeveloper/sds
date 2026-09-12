package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
  primary = BkashPinkLight,
  onPrimary = BkashOnPinkContainerLight,
  primaryContainer = BkashPinkContainerDark,
  onPrimaryContainer = BkashOnPinkContainerDark,
  secondary = BkashPink,
  onSecondary = Color.White,
  background = BkashSurfaceDark,
  surface = BkashCardDark,
  onBackground = BkashTextPrimaryDark,
  onSurface = BkashTextPrimaryDark
)

private val LightColorScheme = lightColorScheme(
  primary = BkashPink,
  onPrimary = Color.White,
  primaryContainer = BkashPinkContainerLight,
  onPrimaryContainer = BkashOnPinkContainerLight,
  secondary = BkashPinkDark,
  onSecondary = Color.White,
  background = BkashSurfaceLight,
  surface = BkashCardLight,
  onBackground = BkashTextPrimaryLight,
  onSurface = BkashTextPrimaryLight
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Keep signature bKash identity consistent
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

