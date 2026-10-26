package com.carmabs.emanate.core.source

import com.carmabs.emanate.core.action.EmaAction
import com.carmabs.emanate.core.configurationOn
import com.carmabs.emanate.core.error.EmaError
import com.carmabs.emanate.core.error.EmaErrorPolicy
import com.carmabs.emanate.core.model.EmaConfiguration
import com.carmabs.emanate.core.model.reflection.Reflection
import com.carmabs.emanate.core.model.reflection.ReflectionData
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
}

private data object ListenerEvent : EmaEvent

private class RecordingListener : EmaSideEffectListener {
    val successes = mutableListOf<Pair<ReflectionData, Duration>>()
    val errors = mutableListOf<Pair<EmaError, Duration>>()
    val finishes = mutableListOf<Pair<Reflection, Duration>>()

    override fun onSuccess(data: ReflectionData, duration: Duration) {
        successes += data to duration
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
        sideEffects {
            add<ListenerAction.Refresh> { delay(2.seconds) }
            add<ListenerAction.Broken> { error("boom") }
            add<ListenerAction.Named>(name = "custom") { }
        }
    }

    @Test
    fun `a side effect is named after the type of its action`() = runTest {
        val listener = RecordingListener()
        val source = source(listener)

        source.dispatch(ListenerAction.Refresh)
        advanceUntilIdle()

        assertEquals(listOf("Refresh"), listener.successes.map { it.first.reflection.methodName })
        assertEquals(listOf("Refresh"), listener.finishes.map { it.first.methodName })
        source.close()
    }

    @Test
    fun `a side effect can have its own name`() = runTest {
        val listener = RecordingListener()
        val source = source(listener)

        source.dispatch(ListenerAction.Named)
        advanceUntilIdle()

        assertEquals(listOf("custom"), listener.finishes.map { it.first.methodName })
        source.close()
    }

    @Test
    fun `the listener receives the time that the side effect took`() = runTest {
        val listener = RecordingListener()
        val source = source(listener)

        source.dispatch(ListenerAction.Refresh)
        advanceUntilIdle()

        // The real time of the test is almost zero: the virtual delay doesn't count, but it can't be negative
        assertTrue(listener.successes.single().second >= Duration.ZERO)
        assertTrue(listener.finishes.single().second >= listener.successes.single().second)
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
        assertEquals("Broken", error.reflection.methodName)
        assertTrue(listener.successes.isEmpty())
        assertEquals(listOf("Broken"), listener.finishes.map { it.first.methodName })
        source.close()
    }
}
