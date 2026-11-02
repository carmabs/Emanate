package com.carmabs.emanate.compose.provider

import androidx.compose.runtime.Composable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel as composeViewModel
import com.carmabs.emanate.android.viewmodel.EmaAndroidViewModel
import com.carmabs.emanate.android.viewmodel.EmaViewModelFactory
import com.carmabs.emanate.core.savestate.EmaStateStore
import com.carmabs.emanate.core.state.EmaEvent
import com.carmabs.emanate.core.state.EmaState
import com.carmabs.emanate.core.viewmodel.EmaViewModelContract

object EmaScreenProvider {

    /**
     * Returns the Android view model of the screen, created with [viewModel] only if it doesn't exist yet.
     *
     * @param key identifies the view model of the screen
     * @param viewModel creates the view model with the store of its backup, over [savedStateHandle] or the one of the
     * screen
     */
    @Composable
    fun <S : EmaState, E : EmaEvent> provideComposableViewModel(
        key: String,
        viewModel: (store: EmaStateStore) -> EmaViewModelContract<S, E>,
        savedStateHandle: SavedStateHandle?
    ): EmaAndroidViewModel<S, E> {
        @Suppress("UNCHECKED_CAST")
        return composeViewModel(
            modelClass = EmaAndroidViewModel::class.java,
            key = key,
            factory = EmaViewModelFactory(viewModel, savedStateHandle)
        ) as EmaAndroidViewModel<S, E>
    }

    /**
     * Key of the view model of the screen drawn by [screenContent]: each screen has its own view model.
     */
    internal fun viewModelKey(screenContent: Any): String = "EmaViewModel of ${screenContent::class.java.name}"
}
