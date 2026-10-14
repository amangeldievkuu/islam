package com.sunnahcompanion.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

private val LocalArabicTypography = staticCompositionLocalOf { arabicTypography(1f) }

@Composable
fun SunnahTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    arabicTextScale: Float = 1f,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    val arabic = remember(arabicTextScale) { arabicTypography(arabicTextScale) }

    CompositionLocalProvider(LocalArabicTypography provides arabic) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content,
        )
    }
}

/** App-specific theme values alongside [MaterialTheme]. */
object SunnahTheme {
    val arabic: ArabicTypography
        @Composable @ReadOnlyComposable
        get() = LocalArabicTypography.current
}
