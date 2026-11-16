package com.carmabs.emanate.compose.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.carmabs.emanate.compose.savestate.SavedStateHandleStore
import com.carmabs.emanate.core.savestate.EmaStateStore
import com.carmabs.emanate.core.viewmodel.EmaViewModelContract
import kotlin.reflect.KClass

/**
 * Interface which all factories of [EmaViewModelHolder] must use to be handled by the library
 *
 * @author <a href="mailto:apps.carmabs@gmail.com">Carlos Mateo Benito</a>
 */
abstract class EmaFactory<VM : EmaViewModelContract<*, *>> : ViewModelProvider.Factory {

    /**
     * Called if the view model has not been created yet
     * @param T View model class
     * @param modelClass Class of the view model
     * @param extras Creation extras of the owner of the view model, which give the saved state of the screen
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
        val savedStateHandle = provideSavedStateHandle() ?: extras.createSavedStateHandle()
        return EmaViewModelHolder(createViewModel(SavedStateHandleStore(savedStateHandle)), savedStateHandle) as T
    }

    /**
     * Creates the view model of the screen.
     *
     * @param store saves the values of the backups of the view model in the SavedStateHandle of the screen
     */
    abstract fun createViewModel(store: EmaStateStore): VM

    abstract fun provideSavedStateHandle(): SavedStateHandle?
}
