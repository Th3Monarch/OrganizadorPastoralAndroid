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
    primary = GoldAccent,
    onPrimary = NavyDeep,
    primaryContainer = NavyPrimary,
    onPrimaryContainer = Color.White,
    secondary = SageGreenLight,
    onSecondary = NavyDeep,
    secondaryContainer = SageGreen,
    onSecondaryContainer = Color.White,
    tertiary = LiturgicalPurple,
    background = SoftBackgroundDark,
    surface = SoftSurfaceDark,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = NavySurface,
    onSurfaceVariant = TextSecondaryDark,
    error = CrimsonRed,
    errorContainer = CrimsonContainer,
    onError = Color.White
  )

private val LightColorScheme =
  lightColorScheme(
    primary = NavyPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = NavyDeep,
    secondary = GoldPrimary,
    onSecondary = Color.White,
    secondaryContainer = GoldContainer,
    onSecondaryContainer = Color(0xFF78350F),
    tertiary = LiturgicalPurple,
    background = SoftBackgroundLight,
    surface = SoftSurfaceLight,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = TextSecondaryLight,
    error = CrimsonRed,
    errorContainer = CrimsonContainer,
    onError = Color.White
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Use our intentional pastoral palette
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

