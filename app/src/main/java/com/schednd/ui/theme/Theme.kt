package com.schednd.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = Color(0xFF252525),
    onPrimaryContainer = Color(0xFFF5F5F5),
    secondary = Color(0xFF8A8A8E),
    onSecondary = Color(0xFF111111),
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = Color(0xFF3A3A3A),
    error = Color(0xFFD43030),
    errorContainer = Color(0xFF2A1515),
    onError = Color.White,
    surfaceContainerHighest = Color(0xFF333333),
    surfaceContainerHigh = Color(0xFF2A2A2A),
    surfaceContainer = DarkSurface,
    surfaceContainerLow = Color(0xFF151515),
    surfaceContainerLowest = DarkBackground,
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = Color(0xFFEAEAEA),
    onPrimaryContainer = Color(0xFF111111),
    secondary = Color(0xFF6E6E73),
    onSecondary = Color.White,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = Color(0xFFE5E5E5),
    error = Color(0xFFD43030),
    errorContainer = Color(0xFFFBE0E0),
    onError = Color.White,
    surfaceContainerHighest = Color(0xFFE0E0E0),
    surfaceContainerHigh = Color(0xFFEAEAEA),
    surfaceContainer = LightSurface,
    surfaceContainerLow = LightBackground,
    surfaceContainerLowest = Color.White,
)

@Composable
fun SchedyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
