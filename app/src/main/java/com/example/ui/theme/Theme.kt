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
    primary = AutoAmber,
    onPrimary = DarkBackground,
    primaryContainer = AutoAmberDeep,
    onPrimaryContainer = AutoAmberSoft,
    secondary = AutoEmeraldBright,
    onSecondary = Color(0xFF042619),
    secondaryContainer = AutoEmeraldDeep,
    onSecondaryContainer = AutoEmeraldSoft,
    tertiary = NightIndigoBright,
    onTertiary = Color(0xFF0E1333),
    tertiaryContainer = NightIndigoDeep,
    onTertiaryContainer = NightIndigoSoft,
    background = DarkBackground,
    onBackground = DarkInk,
    surface = DarkSurface,
    onSurface = DarkInk,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkMutedInk,
    outline = DarkOutline,
    error = AlertCoralBright,
    onError = Color(0xFF3B090A),
  )

private val LightColorScheme =
  lightColorScheme(
    primary = AutoAmberDark,
    onPrimary = Color.White,
    primaryContainer = AutoAmberSoft,
    onPrimaryContainer = AutoAmberDeep,
    secondary = AutoEmerald,
    onSecondary = Color.White,
    secondaryContainer = AutoEmeraldSoft,
    onSecondaryContainer = AutoEmeraldDeep,
    tertiary = NightIndigo,
    onTertiary = Color.White,
    tertiaryContainer = NightIndigoSoft,
    onTertiaryContainer = NightIndigoDeep,
    background = LightBackground,
    onBackground = LightInk,
    surface = LightSurface,
    onSurface = LightInk,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightMutedInk,
    outline = LightOutline,
    error = AlertCoral,
    onError = Color.White,
  )

@Composable
fun AutoMeterTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Default to intentional AutoMeter brand palette; dynamic color optional on Android 12+
  dynamicColor: Boolean = false,
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

  MaterialTheme(
    colorScheme = colorScheme,
    typography = AutoMeterTypography,
    content = content,
  )
}
