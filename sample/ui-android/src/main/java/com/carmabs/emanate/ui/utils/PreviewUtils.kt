package com.carmabs.emanate.ui.utils

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import com.carmabs.emanate.ui.theme.EmaSampleTheme

/**
 * Draws the [content] of a preview as the app does: with the theme and over a surface that gives it the background and
 * the color of the texts, so it is readable in light and dark mode.
 */
@Composable
fun RenderPreview(content: @Composable () -> Unit) {
    EmaSampleTheme {
        Surface(
            color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
            content = content
        )
    }
}
