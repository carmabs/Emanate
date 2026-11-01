package com.carmabs.emanate.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import com.carmabs.emanate.compose.ui.EmaScreenContent
import com.carmabs.emanate.core.action.EmaAction
import com.carmabs.emanate.core.action.EmaActionDispatcher
import com.carmabs.emanate.core.initializer.EmaInitializer
import com.carmabs.emanate.core.state.EmaEvent
import com.carmabs.emanate.core.state.EmaState
import com.carmabs.emanate.core.viewmodel.EmaViewModelAction
import com.carmabs.emanate.core.viewmodel.EmaViewModelClassic
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

data class CounterState(val count: Int = 0) : EmaState

sealed interface CounterAction : EmaAction {
    data object Increment : CounterAction
    data object Back : CounterAction
    data object Notify : CounterAction
}

sealed interface CounterEvent : EmaEvent {
    data object Notified : CounterEvent
}

data class RouteInitializer(val name: String) : EmaInitializer

/**
 * Serializer written by hand, so the tests do not need the serialization plugin.
 */
object RouteInitializerSerializer : KSerializer<RouteInitializer> {
    override val descriptor = PrimitiveSerialDescriptor("RouteInitializer", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: RouteInitializer) = encoder.encodeString(value.name)
    override fun deserialize(decoder: Decoder) = RouteInitializer(decoder.decodeString())
}

class CounterViewModel : EmaViewModelAction<CounterState, CounterAction, CounterEvent>(CounterState()) {
    val hooks = mutableListOf<String>()
    var initializer: EmaInitializer? = null

    override fun onStateCreated(initializer: EmaInitializer?) {
        hooks += "created"
        this.initializer = initializer
    }

    override fun onViewStarted() {
        hooks += "started"
    }

    override fun onViewResumed() {
        hooks += "resumed"
    }

    override fun onViewPaused() {
        hooks += "paused"
    }

    override fun onViewStopped() {
        hooks += "stopped"
    }

    override fun onAction(action: CounterAction) {
        when (action) {
            CounterAction.Increment -> updateState { copy(count = count + 1) }
            CounterAction.Back -> hooks += "back"
            CounterAction.Notify -> publish(CounterEvent.Notified)
        }
    }
}

/**
 * ViewModel that does not handle actions.
 */
class ClassicViewModel : EmaViewModelClassic<CounterState, CounterEvent>(CounterState()) {
    override fun onStateCreated(initializer: EmaInitializer?) = Unit
}

class CounterScreen(private val backEnabled: Boolean = true) : EmaScreenContent<CounterState, CounterAction> {

    override fun onBack(state: CounterState) = if (backEnabled) CounterAction.Back else null

    @Composable
    override fun onState(state: CounterState, actions: EmaActionDispatcher<CounterAction>) {
        Column {
            BasicText("Count ${state.count}")
            BasicText("Increment", Modifier.clickable { actions.dispatch(CounterAction.Increment) })
            BasicText("Notify", Modifier.clickable { actions.dispatch(CounterAction.Notify) })
        }
    }
}

/**
 * Screen that keeps the default back handling.
 */
class SilentScreen : EmaScreenContent<CounterState, CounterAction> {
    @Composable
    override fun onState(state: CounterState, actions: EmaActionDispatcher<CounterAction>) {
        BasicText("Silent ${state.count}", Modifier.clickable { actions.dispatch(CounterAction.Notify) })
    }
}

/**
 * Screen whose button only receives the actions, to count how many times the button is composed.
 */
class ActionsScreen : EmaScreenContent<CounterState, CounterAction> {
    var buttonCompositions = 0

    @Composable
    override fun onState(state: CounterState, actions: EmaActionDispatcher<CounterAction>) {
        Column {
            BasicText("Count ${state.count}")
            IncrementButton(actions) { buttonCompositions++ }
        }
    }
}

@Composable
private fun IncrementButton(actions: EmaActionDispatcher<CounterAction>, onComposed: () -> Unit) {
    SideEffect { onComposed() }
    BasicText("Increment", Modifier.clickable { actions.dispatch(CounterAction.Increment) })
}
