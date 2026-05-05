package com.example.voicetodo.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFE91E63),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFC1E3),
    onPrimaryContainer = Color(0xFF880E4F),
    secondary = Color(0xFFFF80AB),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFD6E0),
    onSecondaryContainer = Color(0xFFC51162),
    tertiary = Color(0xFFFF4081),
    onTertiary = Color.White,
    background = Color(0xFFFFF0F3),
    onBackground = Color(0xFF212121),
    surface = Color.White,
    onSurface = Color(0xFF212121),
    error = Color(0xFFD32F2F),
    onError = Color.White,
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFF80AB),
    onPrimary = Color(0xFF880E4F),
    primaryContainer = Color(0xFFC51162),
    onPrimaryContainer = Color(0xFFFFC1E3),
    secondary = Color(0xFFFF80AB),
    onSecondary = Color(0xFF880E4F),
    secondaryContainer = Color(0xFFAD1457),
    onSecondaryContainer = Color(0xFFFFD6E0),
    tertiary = Color(0xFFFF80AB),
    onTertiary = Color(0xFF880E4F),
    background = Color(0xFF1A0A10),
    onBackground = Color(0xFFE0E0E0),
    surface = Color(0xFF2D1018),
    onSurface = Color(0xFFE0E0E0),
    error = Color(0xFFEF5350),
    onError = Color(0xFFB71C1C),
)

@Composable
fun VoiceTodoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
