package com.example.ui.theme

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = TealPrimaryLight,
    onPrimary = Color(0xFF003732),
    primaryContainer = TealPrimaryDark,
    onPrimaryContainer = TealContainer,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFF8E8E93),
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    secondary = LunchColorDark,
    tertiary = DinnerColorDark
  )

private val LightColorScheme =
  lightColorScheme(
    primary = TealPrimary,
    onPrimary = Color.White,
    primaryContainer = TealContainer,
    onPrimaryContainer = TealOnContainer,
    background = MedicalBackground,
    surface = MedicalSurface,
    surfaceVariant = MedicalSurfaceVariant,
    onBackground = AppleInk,
    onSurface = AppleInk,
    onSurfaceVariant = AppleInkMuted48,
    outline = AppleHairline,
    outlineVariant = AppleDividerSoft,
    secondary = LunchColor,
    tertiary = DinnerColor
  )

@Composable
private fun ColorScheme.animated(durationMillis: Int = 300): ColorScheme {
  val animSpec = tween<Color>(durationMillis = durationMillis)
  return copy(
    primary = animateColorAsState(primary, animSpec, label = "primary").value,
    onPrimary = animateColorAsState(onPrimary, animSpec, label = "onPrimary").value,
    primaryContainer = animateColorAsState(primaryContainer, animSpec, label = "primaryContainer").value,
    onPrimaryContainer = animateColorAsState(onPrimaryContainer, animSpec, label = "onPrimaryContainer").value,
    inversePrimary = animateColorAsState(inversePrimary, animSpec, label = "inversePrimary").value,
    secondary = animateColorAsState(secondary, animSpec, label = "secondary").value,
    onSecondary = animateColorAsState(onSecondary, animSpec, label = "onSecondary").value,
    secondaryContainer = animateColorAsState(secondaryContainer, animSpec, label = "secondaryContainer").value,
    onSecondaryContainer = animateColorAsState(onSecondaryContainer, animSpec, label = "onSecondaryContainer").value,
    tertiary = animateColorAsState(tertiary, animSpec, label = "tertiary").value,
    onTertiary = animateColorAsState(onTertiary, animSpec, label = "onTertiary").value,
    tertiaryContainer = animateColorAsState(tertiaryContainer, animSpec, label = "tertiaryContainer").value,
    onTertiaryContainer = animateColorAsState(onTertiaryContainer, animSpec, label = "onTertiaryContainer").value,
    background = animateColorAsState(background, animSpec, label = "background").value,
    onBackground = animateColorAsState(onBackground, animSpec, label = "onBackground").value,
    surface = animateColorAsState(surface, animSpec, label = "surface").value,
    onSurface = animateColorAsState(onSurface, animSpec, label = "onSurface").value,
    surfaceVariant = animateColorAsState(surfaceVariant, animSpec, label = "surfaceVariant").value,
    onSurfaceVariant = animateColorAsState(onSurfaceVariant, animSpec, label = "onSurfaceVariant").value,
    surfaceTint = animateColorAsState(surfaceTint, animSpec, label = "surfaceTint").value,
    inverseSurface = animateColorAsState(inverseSurface, animSpec, label = "inverseSurface").value,
    inverseOnSurface = animateColorAsState(inverseOnSurface, animSpec, label = "inverseOnSurface").value,
    error = animateColorAsState(error, animSpec, label = "error").value,
    onError = animateColorAsState(onError, animSpec, label = "onError").value,
    errorContainer = animateColorAsState(errorContainer, animSpec, label = "errorContainer").value,
    onErrorContainer = animateColorAsState(onErrorContainer, animSpec, label = "onErrorContainer").value,
    outline = animateColorAsState(outline, animSpec, label = "outline").value,
    outlineVariant = animateColorAsState(outlineVariant, animSpec, label = "outlineVariant").value,
    scrim = animateColorAsState(scrim, animSpec, label = "scrim").value
  )
}

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disable dynamicColor by default to guarantee high-contrast Apple branding in both themes
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val targetColorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  val animatedColorScheme = targetColorScheme.animated(durationMillis = 350)

  CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
    MaterialTheme(colorScheme = animatedColorScheme, typography = Typography, content = content)
  }
}
