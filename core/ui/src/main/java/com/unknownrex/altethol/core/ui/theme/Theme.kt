package com.unknownrex.altethol.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = BrandBlue,
    onPrimary = SurfaceWhite,
    primaryContainer = SurfaceSubtle,
    onPrimaryContainer = BrandBlue,
    secondary = BrandYellow,
    onSecondary = SurfaceWhite,
    background = SurfaceWhite,
    onBackground = TextPrimary,
    surface = SurfaceWhite,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceSubtle,
    onSurfaceVariant = TextSecondary,
    outline = BorderSecondary,
    outlineVariant = BorderPrimary,
    error = ErrorRed,
    onError = SurfaceWhite,
    errorContainer = Color(0xFFFDE8E7),
    onErrorContainer = ErrorRed,
)

@Composable
fun AltEtholTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content,
    )
}
