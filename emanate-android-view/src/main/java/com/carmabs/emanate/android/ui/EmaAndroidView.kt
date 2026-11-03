@file:Suppress("DEPRECATION")

package com.carmabs.emanate.android.ui

import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.carmabs.emanate.android.viewmodel.EmaAndroidViewModel
import com.carmabs.emanate.android.viewmodel.EmaViewModelFactory
import com.carmabs.emanate.core.savestate.EmaStateStore
import com.carmabs.emanate.core.state.EmaEvent
import com.carmabs.emanate.core.state.EmaState
import com.carmabs.emanate.core.view.EmaView
import com.carmabs.emanate.core.viewmodel.EmaViewModelContract

/**
 * View to handle VM view logic states through [EmaState].
 * The user must provide in the constructor by template:
 *  - The view model class [EmaViewModelContract] is going to use the view
 *  - The navigation state class [EmaNavigationEvent] will handle the navigation
 *
 * @author <a href="mailto:apps.carmabs@gmail.com">Carlos Mateo Benito</a>
 */
@Deprecated(
    "Emanate will work only with Compose and EmaViewModel. Build the screens with emanate-compose-android and EmaViewModel instead of Views."
)
interface EmaAndroidView<S : EmaState, VM : EmaViewModelContract<S, E>, E : EmaEvent> : EmaView<S, VM, E> {

    fun initializeViewModel(
        activity: ComponentActivity,
        viewModel: (store: EmaStateStore) -> VM
    ): EmaAndroidViewModel<S, E> {
        val emaFactory = EmaViewModelFactory(viewModel)
        val vm = ViewModelProvider(
            activity,
            emaFactory
        )[viewModelKey, EmaAndroidViewModel::class.java]
        return vm as EmaAndroidViewModel<S, E>
    }

    fun initializeViewModel(fragment: Fragment, viewModel: (store: EmaStateStore) -> VM): EmaAndroidViewModel<S, E> {
        val emaFactory = EmaViewModelFactory(viewModel)
        val vm = ViewModelProvider(
            fragment,
            emaFactory
        )[viewModelKey, EmaAndroidViewModel::class.java]
        return vm as EmaAndroidViewModel<S, E>
    }

    /**
     * Key of the view model of this screen: each screen has its own view model, created only if it doesn't exist yet.
     */
    val viewModelKey: String
        get() = "EmaViewModel of ${this::class.java.name}"
}
