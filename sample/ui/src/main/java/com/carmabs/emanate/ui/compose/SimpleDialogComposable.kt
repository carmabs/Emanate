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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.DialogProperties
import com.carmabs.emanate.compose.extension.stringResource
import com.carmabs.emanate.core.model.EmaText
import com.carmabs.emanate.sample.ui.R
import com.carmabs.emanate.ui.dialog.simple.SimpleDialogData
import com.carmabs.emanate.ui.dialog.simple.SimpleDialogListener
import com.carmabs.emanate.ui.theme.EmaSampleTheme

@Composable
fun SimpleDialogComposable(dialogData: SimpleDialogData, dialogListener: SimpleDialogListener) {
    AlertDialog(
        onDismissRequest = {
            dialogListener.onBackPressed()
        },
        properties = DialogProperties(dismissOnClickOutside = !dialogData.isModal),
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
            TextButton(onClick = { dialogListener.onConfirmClicked() }) {
                Text(text = stringResource(id = R.string.dialog_accept))
            }
        },
        dismissButton = if (dialogData.showCancel) {
            {
                TextButton(onClick = { dialogListener.onCancelClicked() }) {
                    Text(text = stringResource(id = R.string.dialog_cancel))
                }
            }
        } else {
            null
        }
    )
}

//region Previews

@Preview
@Composable
fun PreviewOnDialog() {
    EmaSampleTheme {
        SimpleDialogComposable(
            SimpleDialogData(
                title = EmaText.text("Preview title sample"),
                message = EmaText.text("Preview message sample"),
                image = R.drawable.ic_exit,
                showCancel = true
            ),
            SimpleDialogListener.EMPTY
        )
    }
}

//endregion
