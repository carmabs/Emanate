package com.carmabs.emanate.compose.ui

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import com.carmabs.emanate.compose.action.EmaImmutableActionDispatcher
import com.carmabs.emanate.core.action.EmaAction
import com.carmabs.emanate.core.state.EmaState

/**
 * Draws a screen: renders its state and reports what the user does as actions. The events of the screen are handled
 * where it is created, with the onEvent of [EmaScreen] or emaScreen.
 */
interface EmaScreenContent<S : EmaState, A : EmaAction> {

    /**
     * Action to dispatch when back is pressed. The view model decides how to react to it.
     * @param state current state of the screen
     * @return the action to dispatch, or null to let the system handle the back press
     */
    fun onBack(state: S): A? = null

    @Composable
    @SuppressLint("ComposableNaming")
    fun onState(state: S, actions: EmaImmutableActionDispatcher<A>)
}
