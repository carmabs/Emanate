package com.carmabs.emanate.presentation.splash

import com.carmabs.emanate.core.action.EmaAction
import com.carmabs.emanate.core.initializer.EmaInitializer
import com.carmabs.emanate.core.source.sideeffect.sideEffectOf
import com.carmabs.emanate.core.state.EmaState
import com.carmabs.emanate.presentation.base.BaseViewModel
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

class SplashViewModel : BaseViewModel<EmaState.EMPTY, EmaAction.EMPTY, SplashEvent>(EmaState.EMPTY) {

    // The view is created
    private val sideEffectFinishSplash = sideEffectOf(EmaInitializer::class) {
        delay(1500.milliseconds)
        publish(SplashEvent.SplashFinished)
    }

    override fun setup() = source {
        sideEffects {
            add(sideEffectFinishSplash)
        }
    }
}
