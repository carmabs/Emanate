package com.carmabs.emanate.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.carmabs.emanate.sample.ui.R

/**
 * Theme for compose screens. Colors are read from the palette resources, so XML and compose
 * screens share the same palette, including dark mode.
 */
@Composable
fun EmaSampleTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = paletteColorScheme(isSystemInDarkTheme()),
        typography = sampleTypography(),
        shapes = sampleShapes,
        content = content
    )
}

@Composable
private fun paletteColorScheme(darkTheme: Boolean): ColorScheme {
    val base = if (darkTheme) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = colorResource(R.color.palette_primary),
        onPrimary = colorResource(R.color.palette_onPrimary),
        primaryContainer = colorResource(R.color.palette_primaryContainer),
        onPrimaryContainer = colorResource(R.color.palette_onPrimaryContainer),
        inversePrimary = colorResource(R.color.palette_inversePrimary),
        secondary = colorResource(R.color.palette_secondary),
        onSecondary = colorResource(R.color.palette_onSecondary),
        secondaryContainer = colorResource(R.color.palette_secondaryContainer),
        onSecondaryContainer = colorResource(R.color.palette_onSecondaryContainer),
        background = colorResource(R.color.palette_surface),
        onBackground = colorResource(R.color.palette_onSurface),
        surface = colorResource(R.color.palette_surface),
        onSurface = colorResource(R.color.palette_onSurface),
        surfaceVariant = colorResource(R.color.palette_surfaceContainer),
        onSurfaceVariant = colorResource(R.color.palette_onSurfaceVariant),
        surfaceContainerLowest = colorResource(R.color.palette_surfaceContainerLowest),
        surfaceContainerLow = colorResource(R.color.palette_surfaceContainerLow),
        surfaceContainer = colorResource(R.color.palette_surfaceContainer),
        surfaceContainerHigh = colorResource(R.color.palette_surfaceContainerHigh),
        surfaceContainerHighest = colorResource(R.color.palette_surfaceContainerHighest),
        inverseSurface = colorResource(R.color.palette_inverseSurface),
        inverseOnSurface = colorResource(R.color.palette_inverseOnSurface),
        outline = colorResource(R.color.palette_outline),
        outlineVariant = colorResource(R.color.palette_outlineVariant),
        error = colorResource(R.color.palette_error),
        onError = colorResource(R.color.palette_onError),
        errorContainer = colorResource(R.color.palette_errorContainer),
        onErrorContainer = colorResource(R.color.palette_onErrorContainer)
    )
}

private fun sampleTypography(): Typography {
    val base = Typography()
    return base.copy(
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold)
    )
}

private val sampleShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)
