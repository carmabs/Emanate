package com.carmabs.emanate.ui.compose

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.window.DialogProperties
import com.carmabs.emanate.compose.extension.stringResource
import com.carmabs.emanate.core.model.EmaText
import com.carmabs.emanate.sample.ui.R
import com.carmabs.emanate.ui.dialog.simple.SimpleDialogData
import com.carmabs.emanate.ui.utils.RenderPreview

/**
 * A dialog with an accept button, and a cancel one only when [onCancelClick] is given.
 * @param onConfirmClick the accept button is pressed
 * @param onCancelClick the cancel button is pressed. Without it the dialog has no cancel button
 * @param onDismiss the user presses back or touches outside of it
 */
@Composable
fun SimpleDialogComposable(
    dialogData: SimpleDialogData,
    onConfirmClick: () -> Unit,
    onDismiss: () -> Unit,
    onCancelClick: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = false),
        icon = dialogData.image?.let {
            {
                Icon(
                    painter = painterResource(id = it),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        title = {
            Text(text = dialogData.title.stringResource(), textAlign = TextAlign.Center)
        },
        text = {
            Text(text = dialogData.message.stringResource(), textAlign = TextAlign.Center)
        },
        confirmButton = {
            TextButton(onClick = onConfirmClick) {
                Text(text = stringResource(id = R.string.dialog_accept))
            }
        },
        dismissButton = onCancelClick?.let {
            {
                TextButton(onClick = it) {
                    Text(text = stringResource(id = R.string.dialog_cancel))
                }
            }
        }
    )
}

//region Previews

@PreviewLightDark
@Composable
fun PreviewOnDialog() {
    RenderPreview {
        SimpleDialogComposable(
            dialogData = SimpleDialogData(
                title = EmaText.text("Preview title sample"),
                message = EmaText.text("Preview message sample"),
                image = R.drawable.ic_exit
            ),
            onConfirmClick = {},
            onDismiss = {},
            onCancelClick = {}
        )
    }
}

//endregion
