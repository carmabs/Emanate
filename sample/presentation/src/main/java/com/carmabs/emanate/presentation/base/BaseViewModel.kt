package com.carmabs.emanate.presentation.base

import com.carmabs.emanate.core.Emanate
import com.carmabs.emanate.core.action.EmaAction
import com.carmabs.emanate.core.model.EmaConfiguration
import com.carmabs.emanate.core.state.EmaEvent
import com.carmabs.emanate.core.state.EmaState
import com.carmabs.emanate.core.viewmodel.EmaViewModel

/**
 * The class that every ViewModel of the app extends, instead of extending [EmaViewModel] directly.
 *
 * It adds nothing yet, but it is the layer of the architecture that belongs to the app: the day that every screen
 * needs the same thing, like a plugin installed in all of them, a helper to report errors or a common way of
 * handling a session that expires, it is written here once and all the ViewModels have it, without touching them.
 * Adding it later would mean changing the superclass of every ViewModel at once.
 */
abstract class BaseViewModel<S : EmaState, A : EmaAction, E : EmaEvent>(
    initialDataState: S,
    configuration: EmaConfiguration = Emanate.configuration
) : EmaViewModel<S, A, E>(initialDataState, configuration)
