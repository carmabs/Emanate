package com.carmabs.emanate.android.view.viewmodel

import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.carmabs.emanate.core.savestate.EmaStateStore
import com.carmabs.emanate.core.viewmodel.EmaViewModelContract

/**
 * Interface which all factories of [EmaAndroidViewModel] must use to be handled by the library
 *
 * @author <a href="mailto:apps.carmabs@gmail.com">Carlos Mateo Benito</a>
 */
abstract class EmaFactory<VM : EmaViewModelContract<*, *>> : AbstractSavedStateViewModelFactory() {

    /**
     * Called if the view model has not been created yet
     * @param T View model class
     * @param modelClass Class of the view model
     */
    override fun <T : ViewModel> create(key: String, modelClass: Class<T>, handle: SavedStateHandle): T {
        val savedStateHandle = provideSavedStateHandle() ?: handle
        return EmaAndroidViewModel(createViewModel(SavedStateHandleStore(savedStateHandle)), savedStateHandle) as T
    }

    /**
     * Creates the view model of the screen.
     *
     * @param store saves the values of the backups of the view model in the SavedStateHandle of the screen
     */
    abstract fun createViewModel(store: EmaStateStore): VM

    abstract fun provideSavedStateHandle(): SavedStateHandle?
}
