package com.carmabs.emanate.test

import com.carmabs.emanate.core.action.EmaAction
import com.carmabs.emanate.core.source.EmaSource
import com.carmabs.emanate.core.source.EmaSourceObserver
import com.carmabs.emanate.core.state.EmaEvent
import com.carmabs.emanate.core.state.EmaState
import com.carmabs.emanate.core.viewmodel.EmaViewModel
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Records in order the actions, the states and the events of a source or a view model, to check them in a test.
 *
 * @author <a href=“mailto:apps.carmabs@gmail.com”>Carlos Mateo Benito</a>
 */
class EmaRecorder<S : EmaState, E : EmaEvent> internal constructor() {

    private val recordedActions = CopyOnWriteArrayList<EmaAction>()
    private val recordedStates = CopyOnWriteArrayList<S>()
    private val recordedEvents = CopyOnWriteArrayList<E>()

    /**
     * Actions that entered the source, including the ones blocked by a middleware.
     */
    val actions: List<EmaAction>
        get() = recordedActions.toList()

    /**
     * States returned by the reducers, one for each reduced action.
     */
    val states: List<S>
        get() = recordedStates.toList()

    /**
     * Published events.
     */
    val events: List<E>
        get() = recordedEvents.toList()

    internal val observer = object : EmaSourceObserver<S, E> {
        override fun onAction(action: EmaAction) {
            recordedActions += action
        }

        override fun onState(state: S) {
            recordedStates += state
        }

        override fun onEvent(event: E) {
            recordedEvents += event
        }
    }

    /**
     * Checks that the recorded actions are [expected], in order.
     */
    fun assertActions(vararg expected: EmaAction) {
        assertEqualLists("actions", expected.toList(), actions)
    }

    /**
     * Checks that the recorded states are [expected], in order.
     */
    fun assertStates(vararg expected: S) {
        assertEqualLists("states", expected.toList(), states)
    }

    /**
     * Checks that the last recorded state is [expected].
     */
    fun assertState(expected: S) {
        val last = lastState()
        if (last != expected) throw AssertionError("Expected last state <$expected> but was <$last>")
    }

    /**
     * Checks that the last recorded state fulfils [predicate].
     */
    fun assertState(predicate: (S) -> Boolean) {
        val last = lastState()
        if (!predicate(last)) throw AssertionError("The last state <$last> doesn't fulfil the condition")
    }

    /**
     * Checks that the published events are [expected], in order.
     */
    fun assertEvents(vararg expected: E) {
        assertEqualLists("events", expected.toList(), events)
    }

    /**
     * Checks that no event has been published.
     */
    fun assertNoEvents() {
        assertEqualLists("events", emptyList(), events)
    }

    private fun lastState(): S = states.lastOrNull() ?: throw AssertionError("No state has been recorded")

    private fun assertEqualLists(name: String, expected: List<Any?>, actual: List<Any?>) {
        if (expected != actual) throw AssertionError("Expected $name <$expected> but were <$actual>")
    }
}

/**
 * Starts recording the source. Call it before dispatching the actions to record.
 */
fun <S : EmaState, A : EmaAction, E : EmaEvent> EmaSource<S, A, E>.record(): EmaRecorder<S, E> =
    EmaRecorder<S, E>().also { addObserver(it.observer) }

/**
 * Starts recording the view model. Call it before its first action to record everything. Create the view model after
 * [useTestConfiguration], so it runs on the dispatcher of the test.
 */
fun <S : EmaState, A : EmaAction, E : EmaEvent> EmaViewModel<S, A, E>.record(): EmaRecorder<S, E> =
    EmaRecorder<S, E>().also { addObserver(it.observer) }
