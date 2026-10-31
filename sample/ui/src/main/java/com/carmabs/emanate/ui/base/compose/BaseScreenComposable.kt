package com.carmabs.emanate.ui.base.compose

import androidx.compose.runtime.Composable
import com.carmabs.emanate.compose.ui.EmaComposableScreenContent
import com.carmabs.emanate.core.action.EmaAction
import com.carmabs.emanate.core.model.EmaText
import com.carmabs.emanate.core.state.EmaState
import com.carmabs.emanate.sample.ui.R
import com.carmabs.emanate.ui.compose.ErrorDialogComposable
import com.carmabs.emanate.ui.compose.LoadingDialogComposable
import com.carmabs.emanate.ui.compose.SimpleDialogComposable
import com.carmabs.emanate.ui.dialog.error.ErrorDialogData
import com.carmabs.emanate.ui.dialog.error.ErrorDialogListener
import com.carmabs.emanate.ui.dialog.loading.LoadingDialogData
import com.carmabs.emanate.ui.dialog.simple.SimpleDialogData
import com.carmabs.emanate.ui.dialog.simple.SimpleDialogListener

abstract class BaseScreenComposable<S : EmaState, A : EmaAction> :
    EmaComposableScreenContent<S, A> {

    @Composable
    protected fun ShowDialog(data: SimpleDialogData, listener: SimpleDialogListener) {
        SimpleDialogComposable(dialogData = data, listener)
    }

    @Composable
    protected fun ShowError(data: ErrorDialogData, listener: ErrorDialogListener) {
        ErrorDialogComposable(dialogData = data, dialogListener = listener)
    }

    @Composable
    protected fun ShowLoading(loadingDialogData: LoadingDialogData? = null) {
        LoadingDialogComposable(
            dialogData = loadingDialogData ?: LoadingDialogData(
                title = EmaText.id(id = R.string.dialog_loading_title),
                message = EmaText.id(id = R.string.dialog_loading_message)
            )
        )
    }
}
