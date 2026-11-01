package com.carmabs.emanate.presentation.choice

import com.carmabs.emanate.core.state.EmaState
import com.carmabs.emanate.presentation.base.BaseViewModel

/**
 * View model of the first screen, where the implementation of the rest of view models is chosen
 */
class ImplementationChoiceViewModel(private val selector: ViewModelImplementationSelector) :
    BaseViewModel<EmaState.EMPTY, ImplementationChoiceAction, ImplementationChoiceEvent>(EmaState.EMPTY) {

    override fun setup() = source {
        sideEffects {
            add<ImplementationChoiceAction.ImplementationSelected> { action ->
                selector.implementation = action.implementation
                publish(ImplementationChoiceEvent.ImplementationChosen)
            }
        }
    }
}
