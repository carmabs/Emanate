package com.carmabs.emanate.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.carmabs.emanate.ui.environment.AppEnvironment
import com.carmabs.emanate.ui.environment.EnvironmentTone
import com.carmabs.emanate.ui.utils.RenderPreview

/**
 * A small pill with the name of the environment, to know which build is running, or nothing when its tone is
 * invisible. The screen that shows it places it and takes care of the system bars
 */
@Composable
fun EnvironmentLabel(environment: AppEnvironment, modifier: Modifier = Modifier) {
    val colors = when (environment.tone) {
        EnvironmentTone.Tertiary ->
            MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer

        EnvironmentTone.Secondary ->
            MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer

        // The real build doesn't tell the user about the environment
        EnvironmentTone.Invisible -> return
    }
    Surface(
        modifier = modifier.padding(end = 16.dp, bottom = 8.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = colors.first,
        contentColor = colors.second
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
            text = environment.name.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@PreviewLightDark
@Composable
private fun PreviewEnvironmentLabel() {
    RenderPreview {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            EnvironmentLabel(AppEnvironment("mock", tone = EnvironmentTone.Tertiary))
            EnvironmentLabel(AppEnvironment("dev", tone = EnvironmentTone.Secondary))
        }
    }
}
