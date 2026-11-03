package com.carmabs.emanate.ui.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.carmabs.emanate.compose.extension.stringResource
import com.carmabs.emanate.core.model.EmaText
import com.carmabs.emanate.ui.dialog.loading.LoadingDialogData
import com.carmabs.emanate.ui.theme.EmaSampleTheme

@Composable
fun LoadingDialogComposable(dialogData: LoadingDialogData) {
    Dialog(onDismissRequest = {}) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = dialogData.title.stringResource(),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = dialogData.message.stringResource(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

//region Previews

@Preview
@Composable
fun PreviewLoadingDialog() {
    EmaSampleTheme {
        LoadingDialogComposable(
            LoadingDialogData(
                title = EmaText.text("Preview title sample"),
                message = EmaText.text("Preview message sample")
            )
        )
    }
}

//endregion
