package com.carmabs.emanate.android.view.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.carmabs.emanate.core.savestate.EmaStateStore
import com.carmabs.emanate.core.viewmodel.EmaViewModelContract

/**
 * Creates the view model of a screen with [viewModel], only if the screen doesn't have it yet.
 *
 * @param viewModel creates the view model with the store of its backup, over the SavedStateHandle of the screen
 *
 * @author <a href="mailto:apps.carmabs@gmail.com">Carlos Mateo Benito</a>
 */
class EmaViewModelFactory<VM : EmaViewModelContract<*, *>>(
    private val viewModel: (store: EmaStateStore) -> VM,
    private val savedStateHandle: SavedStateHandle? = null
) : EmaFactory<VM>() {

    /**
     * @return View model instance
     */
    override fun createViewModel(store: EmaStateStore): VM = viewModel(store)

    override fun provideSavedStateHandle(): SavedStateHandle? = savedStateHandle
}
