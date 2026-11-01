package com.carmabs.emanate.presentation.splash

import com.carmabs.emanate.core.action.EmaAction
import com.carmabs.emanate.core.initializer.EmaInitializer
import com.carmabs.emanate.core.state.EmaState
import com.carmabs.emanate.presentation.base.BaseActionViewModel
import kotlinx.coroutines.delay

class SplashActionViewModel : BaseActionViewModel<EmaState.EMPTY, EmaAction.EMPTY, SplashEvent>(EmaState.EMPTY) {
    override fun onAction(action: EmaAction.EMPTY) = Unit

    override fun onStateCreated(initializer: EmaInitializer?) {
        sideEffect {
            delay(1500)
            publish(SplashEvent.SplashFinished)
        }
    }
}
