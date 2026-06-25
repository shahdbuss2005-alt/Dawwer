package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = LeafGreen,
    secondary = LimePulse,
    tertiary = GoldEarth,
    background = EcoBlack,
    surface = CardBackground,
    onPrimary = EcoBlack,
    onSecondary = EcoBlack,
    onTertiary = EcoBlack,
    onBackground = CreamPaper,
    onSurface = MistWhite,
    error = DangerRust
  )

private val LightColorScheme =
  lightColorScheme(
    primary = ForestMid,
    secondary = ForestDeep,
    tertiary = GoldEarth,
    background = Color(0xFFF7FDF8), // Very soft warm minty white
    surface = Color(0xFFEBF5EE), // Soft light green/mint surface
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = EcoBlack,
    onBackground = EcoBlack,
    onSurface = ForestDeep,
    error = DangerRust
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
