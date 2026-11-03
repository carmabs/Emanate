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
import com.carmabs.emanate.ui.dialog.error.ErrorDialogData
import com.carmabs.emanate.ui.dialog.error.ErrorDialogListener
import com.carmabs.emanate.ui.theme.EmaSampleTheme

@Composable
fun ErrorDialogComposable(dialogData: ErrorDialogData, dialogListener: ErrorDialogListener) {
    AlertDialog(
        onDismissRequest = {
            dialogListener.onBackPressed()
        },
        properties = DialogProperties(dismissOnClickOutside = !dialogData.isModal),
        icon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_error),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
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
        }
    )
}

//region Previews

@Preview
@Composable
fun PreviewOnError() {
    EmaSampleTheme {
        ErrorDialogComposable(
            ErrorDialogData(
                title = EmaText.text("Error"),
                message = EmaText.text("Preview message sample")
            ),
            object : ErrorDialogListener {
                override fun onConfirmClicked() = Unit
                override fun onBackPressed() = Unit
            }
        )
    }
}

//endregion
