package com.carmabs.emanate.core.viewmodel

import com.carmabs.emanate.core.Emanate
import com.carmabs.emanate.core.action.EmaAction
import com.carmabs.emanate.core.broadcast.EmaActionBroadcaster
import com.carmabs.emanate.core.configurationOn
import com.carmabs.emanate.core.error.EmaError
import com.carmabs.emanate.core.error.EmaErrorPolicy
import com.carmabs.emanate.core.initializer.EmaInitializer
import com.carmabs.emanate.core.model.EmaConfiguration
import com.carmabs.emanate.core.state.EmaEvent
import com.carmabs.emanate.core.state.EmaState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

private data class BrokenState(val count: Int = 0) : EmaState

private sealed interface BrokenAction : EmaAction {
    data object Increment : BrokenAction
    data object Broken : BrokenAction
}

private data class Ping(val id: Int, val broken: Boolean = false) : EmaAction

/**
 * Classic view model whose code called by Emanate throws exceptions
 */
private class BrokenViewModel(configuration: EmaConfiguration) :
    EmaViewModelAction<BrokenState, BrokenAction, EmaEvent.EMPTY>(BrokenState(), configuration) {

    val received = mutableListOf<String>()

    override fun onStateCreated(initializer: EmaInitializer?) {
        error("broken onStateCreated")
    }

    override fun onBroadcastListenerSetup() {
        registerBroadcastListener<Ping> { ping ->
            if (ping.broken) error("broken listener")
            received += "ping ${ping.id}"
        }
    }

    override fun onAction(action: BrokenAction) {
        when (action) {
            BrokenAction.Increment -> updateState { copy(count = count + 1) }

            BrokenAction.Broken -> {
                updateState { copy(count = 10) }
                error("broken action")
            }
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class EmaViewModelClassicErrorTest {

    private val errors = mutableListOf<EmaError>()

    private val catchErrors = EmaConfiguration(errorPolicy = EmaErrorPolicy.Catch { errors += it }, logger = null)

    private fun TestScope.brokenViewModel(configuration: EmaConfiguration = catchErrors) =
        BrokenViewModel(configurationOn(UnconfinedTestDispatcher(testScheduler), configuration))

    @Test
    fun `an action that fails is reported and the view model keeps working`() = runTest {
        val viewModel = brokenViewModel()

        viewModel.dispatch(BrokenAction.Broken)
        viewModel.dispatch(BrokenAction.Increment)

        assertEquals("action Broken", errors.single().reflection.name)
        // The state is changed imperatively, so the change made before the error is kept
        assertEquals(BrokenState(count = 11), viewModel.stateFlow.value)
        viewModel.close()
    }

    @Test
    fun `a lifecycle hook that fails is reported and the next ones run`() = runTest {
        val viewModel = brokenViewModel()

        viewModel.onCreated(null)

        assertEquals(listOf("onStateCreated"), errors.map { it.reflection.name })
        assertEquals("BrokenViewModel", errors.single().reflection.containerClassName)
        viewModel.close()
    }

    @Test
    fun `a broadcast listener keeps receiving after an error`() = runTest {
        val viewModel = brokenViewModel()
        viewModel.onCreated(null)

        EmaActionBroadcaster.Shared.broadcast(Ping(1, broken = true))
        EmaActionBroadcaster.Shared.broadcast(Ping(2))
        runCurrent()

        assertEquals(listOf("ping 2"), viewModel.received)
        assertEquals("broadcast listener of Ping", errors.last().reflection.name)
        viewModel.close()
    }

    @Test
    fun `a broadcast listener receives the actions of the broadcaster of its configuration`() = runTest {
        val broadcaster = EmaActionBroadcaster()
        val viewModel = brokenViewModel(catchErrors.copy(broadcaster = broadcaster))
        viewModel.onCreated(null)

        EmaActionBroadcaster.Shared.broadcast(Ping(1))
        broadcaster.broadcast(Ping(2))
        runCurrent()

        assertEquals(listOf("ping 2"), viewModel.received)
        viewModel.close()
    }

    @Test
    fun `the Throw policy throws the exception of an action again`() = runTest {
        val viewModel = brokenViewModel(EmaConfiguration(errorPolicy = EmaErrorPolicy.Throw, logger = null))

        val exception = assertFailsWith<IllegalStateException> { viewModel.dispatch(BrokenAction.Broken) }

        assertEquals("broken action", exception.message)
        viewModel.close()
    }
}
