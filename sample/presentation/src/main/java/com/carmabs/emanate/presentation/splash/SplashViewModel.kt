package com.carmabs.emanate.presentation.splash

import com.carmabs.emanate.core.action.EmaAction
import com.carmabs.emanate.core.initializer.EmaInitializer
import com.carmabs.emanate.core.state.EmaState
import com.carmabs.emanate.core.viewmodel.EmaViewModel
import kotlinx.coroutines.delay

class SplashViewModel : EmaViewModel<EmaState.EMPTY, EmaAction.EMPTY, SplashEvent>(EmaState.EMPTY) {

    override fun setup() = source {
        sideEffects {
            // The view is created
            add<EmaInitializer> {
                delay(1500)
                publish(SplashEvent.SplashFinished)
            }
        }
    }
}
