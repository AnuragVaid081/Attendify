package com.attendify.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AttendifyPrimary,
    onPrimary = Color.White,
    primaryContainer = AttendifyPrimaryDark,
    secondary = AttendifySecondary,
    onSecondary = Color.White,
    tertiary = AttendifyTertiary,
    background = DarkBackground,
    onBackground = OnDarkBackground,
    surface = DarkSurface,
    onSurface = OnDarkBackground,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = OnDarkSurface,
    error = ColorAbsent,
    outline = Gray600
)

private val LightColorScheme = lightColorScheme(
    primary = AttendifyPrimary,
    onPrimary = Color.White,
    primaryContainer = LightSurfaceVariant,
    secondary = AttendifySecondary,
    onSecondary = Color.White,
    tertiary = AttendifyTertiary,
    background = LightBackground,
    onBackground = Gray800,
    surface = LightSurface,
    onSurface = Gray800,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Gray600,
    error = ColorAbsent,
    outline = Gray400
)

@Composable
fun AttendifyTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AttendifyTypography,
        shapes = AttendifyShapes,
        content = content
    )
}
