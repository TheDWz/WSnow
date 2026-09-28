package com.wsnow.app.ui.theme

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

private val LightColors = lightColorScheme(
    primary = Color(0xFF1E5AA8),
    secondary = Color(0xFF4F6B8A),
    tertiary = Color(0xFFB8860B),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9EC5FF),
    secondary = Color(0xFFB7C8DE),
    tertiary = Color(0xFFFFC857),
)

/** Colours cycled through for found-word highlights. */
val HighlightColors = listOf(
    Color(0xFFFF6B6B), Color(0xFF4ECDC4), Color(0xFFFFC857), Color(0xFF6A8CFF),
    Color(0xFFB388EB), Color(0xFF7BD389), Color(0xFFFF9F43), Color(0xFFF78FB3),
)

@Composable
fun WSnowTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        dark -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors, content = content)
}
