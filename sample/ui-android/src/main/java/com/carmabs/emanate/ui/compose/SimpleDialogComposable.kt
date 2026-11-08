package com.carmabs.emanate.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.carmabs.emanate.compose.extension.stringResource
import com.carmabs.emanate.core.model.EmaText
import com.carmabs.emanate.sample.ui.R
import com.carmabs.emanate.ui.dialog.simple.SimpleDialogData
import com.carmabs.emanate.ui.utils.RenderPreview

/**
 * A dialog with an accept button, and a cancel one only when [onCancelClick] is given. The image goes in a round badge
 * above the texts, and the buttons are side by side with the same width.
 * @param onConfirmClick the accept button is pressed
 * @param onCancelClick the cancel button is pressed. Without it the dialog has no cancel button
 * @param onDismiss the user presses back or touches outside of it
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleDialogComposable(
    dialogData: SimpleDialogData,
    onConfirmClick: () -> Unit,
    onDismiss: () -> Unit,
    onCancelClick: (() -> Unit)? = null
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                dialogData.image?.let {
                    val badge = if (dialogData.isError) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.primaryContainer
                    val tint = if (dialogData.isError) MaterialTheme.colorScheme.onErrorContainer
                    else MaterialTheme.colorScheme.onPrimaryContainer
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(badge, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            modifier = Modifier.size(32.dp),
                            painter = painterResource(id = it),
                            contentDescription = null,
                            tint = tint
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
                Text(
                    text = dialogData.title.stringResource(),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Text(
                    modifier = Modifier.padding(top = 8.dp),
                    text = dialogData.message.stringResource(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    onCancelClick?.let {
                        OutlinedButton(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            onClick = it
                        ) {
                            Text(text = stringResource(id = R.string.dialog_cancel))
                        }
                    }
                    Button(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        onClick = onConfirmClick
                    ) {
                        Text(text = stringResource(id = R.string.dialog_accept))
                    }
                }
            }
        }
    }
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

@PreviewLightDark
@Composable
fun PreviewOnErrorDialog() {
    RenderPreview {
        SimpleDialogComposable(
            dialogData = SimpleDialogData(
                title = EmaText.text("Preview error title"),
                message = EmaText.text("Preview error message sample"),
                image = R.drawable.ic_error,
                isError = true
            ),
            onConfirmClick = {},
            onDismiss = {}
        )
    }
}

//endregion
