package com.amoledwatchfaces.solarpath.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Typography
import androidx.wear.compose.material3.dynamicColorScheme

val appColorScheme = ColorScheme(
    primary = primary,
    primaryDim = primaryDim,
    onPrimary = onPrimary,
    primaryContainer = primaryContainer,
    onPrimaryContainer = onPrimaryContainer,

    secondary = secondary,
    secondaryDim = secondaryDim,
    onSecondary = onSecondary,
    secondaryContainer = secondaryContainer,
    onSecondaryContainer = onSecondaryContainer,

    tertiary = tertiary,
    tertiaryDim = tertiaryDim,
    onTertiary = onTertiary,
    tertiaryContainer = tertiaryContainer,
    onTertiaryContainer = onTertiaryContainer,

    surfaceContainerHigh = surfaceContainerHigh,
    surfaceContainer = surfaceContainer,
    surfaceContainerLow = surfaceContainerLow,
    onSurface = onSurface,
    onSurfaceVariant = onSurfaceVariant,

    background = background,
    onBackground = onBackground,

    outline = outline,
    outlineVariant = outlineVariant,

    error = error,
    errorDim = errorDim,
    onError = onError,
    errorContainer = errorContainer,
    onErrorContainer = onErrorContainer,
)

val appTypography = Typography()

@Composable
fun SolarPathAppTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    MaterialTheme(
        colorScheme = dynamicColorScheme(context) ?: appColorScheme,
        typography = appTypography,
        content = content
    )
}
