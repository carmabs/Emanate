package com.carmabs.emanate.core.source

import com.carmabs.emanate.core.action.EmaAction
import com.carmabs.emanate.core.configurationOn
import com.carmabs.emanate.core.error.EmaError
import com.carmabs.emanate.core.error.EmaErrorPolicy
import com.carmabs.emanate.core.model.EmaConfiguration
import com.carmabs.emanate.core.model.reflection.Reflection
import com.carmabs.emanate.core.sideeffect.SideEffectOutput
import com.carmabs.emanate.core.sideeffect.EmaSideEffectListener
import com.carmabs.emanate.core.state.EmaEvent
import com.carmabs.emanate.core.state.EmaState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

private data object ListenerState : EmaState

private sealed interface ListenerAction : EmaAction {
    data object Refresh : ListenerAction
    data object Broken : ListenerAction
    data object Named : ListenerAction
    data object Chain : ListenerAction
    data object Typed : ListenerAction
    data class Result(val id: Int) : ListenerAction
}

private data object ListenerEvent : EmaEvent

private data class ListenerBroadcast(val id: Int) : EmaAction

private class RecordingListener : EmaSideEffectListener {
    val successes = mutableListOf<Triple<Reflection, SideEffectOutput, Duration>>()
    val errors = mutableListOf<Pair<EmaError, Duration>>()
    val finishes = mutableListOf<Pair<Reflection, Duration>>()

    override fun onSuccess(reflection: Reflection, output: SideEffectOutput, duration: Duration) {
        successes += Triple(reflection, output, duration)
    }

    override fun onError(error: EmaError, duration: Duration) {
        errors += error to duration
    }

    override fun onFinish(reflection: Reflection, duration: Duration) {
        finishes += reflection to duration
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class EmaSourceSideEffectListenerTest {

    private fun TestScope.source(
        listener: RecordingListener,
        errorPolicy: EmaErrorPolicy = EmaErrorPolicy.Catch()
    ) = EmaSource<ListenerState, ListenerAction, ListenerEvent>(
        ListenerState,
        configurationOn(
            UnconfinedTestDispatcher(testScheduler),
            EmaConfiguration(sideEffectListener = listener, errorPolicy = errorPolicy, logger = null)
        )
    ) {
        middlewares {
            add<ListenerAction.Typed> { nextAfter { } }
        }
        sideEffects {
            add<ListenerAction.Refresh> { delay(2.seconds) }
            add<ListenerAction.Broken> { error("boom") }
            add<ListenerAction.Named>(name = "custom") { }
            add<ListenerAction.Chain> {
                dispatch(ListenerAction.Result(1))
                postEvent(ListenerEvent)
                broadcast(ListenerBroadcast(2))
                dispatch(ListenerAction.Result(3))
            }
        }
    }

    @Test
    fun `a side effect is named after the type of its action`() = runTest {
        val listener = RecordingListener()
        val source = source(listener)

        source.dispatch(ListenerAction.Refresh)
        advanceUntilIdle()

        assertEquals(listOf("Refresh"), listener.successes.map { it.first.name })
        assertEquals(listOf("Refresh"), listener.finishes.map { it.first.name })
        source.close()
    }

    @Test
    fun `the listener receives what the side effect has launched`() = runTest {
        val listener = RecordingListener()
        val source = source(listener)

        source.dispatch(ListenerAction.Chain)
        advanceUntilIdle()

        val output = listener.successes.single { it.first.name == "Chain" }.second
        assertEquals(listOf(ListenerAction.Result(1), ListenerAction.Result(3)), output.actions)
        assertEquals(listOf(ListenerBroadcast(2)), output.broadcasts)
        assertEquals(listOf(ListenerEvent), output.events)
        source.close()
    }

    @Test
    fun `the output of a side effect doesn't include what other side effects launch`() = runTest {
        val listener = RecordingListener()
        val source = source(listener)

        source.dispatch(ListenerAction.Chain)
        source.dispatch(ListenerAction.Named)
        advanceUntilIdle()

        assertEquals(SideEffectOutput(), listener.successes.single { it.first.name == "custom" }.second)
        source.close()
    }

    @Test
    fun `a nextAfter is named after the action of its middleware`() = runTest {
        val listener = RecordingListener()
        val source = source(listener)

        source.dispatch(ListenerAction.Typed)
        advanceUntilIdle()

        assertEquals(listOf("nextAfter Typed"), listener.finishes.map { it.first.name })
        source.close()
    }

    @Test
    fun `a side effect can have its own name`() = runTest {
        val listener = RecordingListener()
        val source = source(listener)

        source.dispatch(ListenerAction.Named)
        advanceUntilIdle()

        assertEquals(listOf("custom"), listener.finishes.map { it.first.name })
        source.close()
    }

    @Test
    fun `the listener receives the time that the side effect took`() = runTest {
        val listener = RecordingListener()
        val source = source(listener)

        source.dispatch(ListenerAction.Refresh)
        advanceUntilIdle()

        // The real time of the test is almost zero: the virtual delay doesn't count, but it can't be negative
        assertTrue(listener.successes.single().third >= Duration.ZERO)
        assertTrue(listener.finishes.single().second >= listener.successes.single().third)
        source.close()
    }

    @Test
    fun `the listener receives the errors with Catch and the side effect finishes`() = runTest {
        val listener = RecordingListener()
        val source = source(listener)

        source.dispatch(ListenerAction.Broken)
        advanceUntilIdle()

        val (error, _) = listener.errors.single()
        assertEquals("boom", error.exception.message)
        assertEquals("Broken", error.reflection.name)
        assertTrue(listener.successes.isEmpty())
        assertEquals(listOf("Broken"), listener.finishes.map { it.first.name })
        source.close()
    }
}
