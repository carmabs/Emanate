package com.carmabs.emanate.ui.compose

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.carmabs.emanate.sample.ui.R
import com.carmabs.emanate.ui.utils.RenderPreview

@Composable
fun AppButton(
    text: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        modifier = modifier.height(dimensionResource(id = R.dimen.button_height)),
        enabled = enabled,
        onClick = onClick
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

//region Previews

@PreviewLightDark
@Composable
fun PreviewAppButton() {
    RenderPreview {
        AppButton(text = "Test") {
        }
    }
}

//endregion
