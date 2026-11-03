package com.carmabs.emanate.core.source

import com.carmabs.emanate.core.action.EmaAction
import com.carmabs.emanate.core.configurationOn
import com.carmabs.emanate.core.source.reducer.ReducerSetupScope
import com.carmabs.emanate.core.source.reducer.reducerOf
import com.carmabs.emanate.core.state.EmaEvent
import com.carmabs.emanate.core.state.EmaState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

private data class CounterState(val count: Int = 0) : EmaState

private sealed interface CounterAction : EmaAction {
    data object Increment : CounterAction
    data object Decrement : CounterAction
    data class Set(val count: Int) : CounterAction
}

private sealed interface CounterEvent : EmaEvent

@OptIn(ExperimentalCoroutinesApi::class)
class EmaSourceTest {

    private fun TestScope.counterSource() = EmaSource<CounterState, CounterAction, CounterEvent>(
        CounterState(),
        configurationOn(UnconfinedTestDispatcher(testScheduler))
    ) {
        reducers {
            add<CounterAction> { action ->
                when (action) {
                    CounterAction.Increment -> state.copy(count = state.count + 1)
                    CounterAction.Decrement -> state.copy(count = state.count - 1)
                    is CounterAction.Set -> state
                }
            }
        }
        // A second block adds its reducers after the ones of the first block
        reducers {
            add(reducerOf(CounterAction.Set::class) { action -> state.copy(count = action.count) })
        }
    }

    @Test
    fun `the source works without a view model`() = runTest {
        val source = counterSource()

        source.dispatch(CounterAction.Increment)
        source.dispatch(CounterAction.Increment)
        source.dispatch(CounterAction.Decrement)
        runCurrent()

        assertEquals(CounterState(count = 1), source.stateFlow.value)
        source.close()
    }

    @Test
    fun `the reducers run in order and only with the actions of their type`() = runTest {
        val source = counterSource()

        source.dispatch(CounterAction.Increment)
        source.dispatch(CounterAction.Set(10))
        runCurrent()

        assertEquals(CounterState(count = 10), source.state)
        source.close()
    }

    private fun TestScope.countAfterIncrement(setup: ReducerSetupScope<CounterState>.() -> Unit): Int {
        val source = EmaSource<CounterState, CounterAction, CounterEvent>(
            CounterState(),
            configurationOn(UnconfinedTestDispatcher(testScheduler))
        ) {
            reducers(setup)
        }
        source.dispatch(CounterAction.Increment)
        runCurrent()
        val count = source.state.count
        source.close()
        return count
    }

    @Test
    fun `the reducers already created are added alone or joined with plus or from a collection in order`() = runTest {
        val double =
            reducerOf<CounterState, CounterAction.Increment>(CounterAction.Increment::class) {
                state.copy(
                    count =
                        state.count * 2
                )
            }
        val addOne =
            reducerOf<CounterState, CounterAction.Increment>(CounterAction.Increment::class) {
                state.copy(
                    count =
                        state.count + 1
                )
            }
        val addTen =
            reducerOf<CounterState, CounterAction.Increment>(CounterAction.Increment::class) {
                state.copy(
                    count =
                        state.count + 10
                )
            }

        // (0 * 2) + 1 = 1, and the other way round (0 + 1) * 2 = 2
        assertEquals(1, countAfterIncrement { add(double, addOne) })
        assertEquals(2, countAfterIncrement { add(addOne, double) })
        assertEquals(1, countAfterIncrement { add(double + addOne) })
        assertEquals(11, countAfterIncrement { add(double + addOne + addTen) })
        assertEquals(11, countAfterIncrement { add(listOf(double, addOne, addTen)) })
        assertEquals(
            11,
            countAfterIncrement {
                add(double)
                add(addOne + addTen)
            }
        )
    }

    @Test
    fun `observers receive the actions and states in order`() = runTest {
        val source = counterSource()
        val observed = mutableListOf<String>()
        source.addObserver(object : EmaSourceObserver<CounterState, CounterEvent> {
            override fun onAction(action: EmaAction) {
                observed += "action $action"
            }

            override fun onState(state: CounterState) {
                observed += "state $state"
            }
        })

        source.dispatch(CounterAction.Increment)
        source.dispatch(CounterAction.Set(5))
        runCurrent()

        assertEquals(
            listOf(
                "action Increment",
                "state CounterState(count=1)",
                "action Set(count=5)",
                "state CounterState(count=5)"
            ),
            observed
        )
        source.close()
    }

    @Test
    fun `closing the source ignores new actions`() = runTest {
        val source = counterSource()

        source.dispatch(CounterAction.Increment)
        val closed = source.close()
        source.dispatch(CounterAction.Increment)
        runCurrent()

        assertTrue(closed.isCompleted)
        assertEquals(CounterState(count = 1), source.stateFlow.value)
    }

    @Test
    fun `the source can't be configured after setup`() = runTest {
        lateinit var setupScope: SourceSetupScope<CounterState, CounterAction, CounterEvent>
        lateinit var reducerSetupScope: ReducerSetupScope<CounterState>
        val source = EmaSource<CounterState, CounterAction, CounterEvent>(
            CounterState(),
            configurationOn(UnconfinedTestDispatcher(testScheduler))
        ) {
            setupScope = this
            reducers { reducerSetupScope = this }
        }

        assertFailsWith<IllegalStateException> {
            setupScope.reducers { }
        }
        assertFailsWith<IllegalStateException> {
            reducerSetupScope.add<CounterAction> { state }
        }
        source.close()
    }
}
