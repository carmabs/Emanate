package com.carmabs.emanate.test

import com.carmabs.emanate.core.Emanate
import com.carmabs.emanate.core.action.EmaAction
import com.carmabs.emanate.core.initializer.EmaInitializer
import com.carmabs.emanate.core.savestate.EmaRestoreState
import com.carmabs.emanate.core.savestate.EmaStateStore
import com.carmabs.emanate.core.savestate.backupOf
import com.carmabs.emanate.core.state.EmaEvent
import com.carmabs.emanate.core.state.EmaState
import com.carmabs.emanate.core.viewmodel.EmaViewModel
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

private data class SearchState(val query: String = "", val results: List<String> = emptyList()) : EmaState

private sealed interface SearchAction : EmaAction {
    data class QueryWritten(val query: String) : SearchAction
}

private class SearchViewModel(private val store: EmaStateStore) :
    EmaViewModel<SearchState, SearchAction, EmaEvent.EMPTY>(
        initialDataState = SearchState(),
        configuration = Emanate.configuration.copy(logger = null)
    ) {
    override fun setup() = source {
        plugins {
            add(
                backupOf(store) {
                    add("query", save = { query }, restore = { copy(query = it) })
                }
            )
        }
        reducers {
            add<SearchAction.QueryWritten> { action -> state.copy(query = action.query) }
            add<EmaRestoreState> { state.copy(results = listOf("restored")) }
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class TestStateStoreTest {

    @BeforeTest
    fun setUp() {
        useTestConfiguration()
    }

    @AfterTest
    fun tearDown() {
        resetTestConfiguration()
    }

    @Test
    fun `the store keeps the values saved by the view model`() = runTest {
        val store = testStateStore()
        val viewModel = SearchViewModel(store)

        viewModel.onCreated(null)
        viewModel.dispatch(SearchAction.QueryWritten("ema"))
        advanceUntilIdle()

        assertEquals(mapOf("query" to "ema"), store.values)
        viewModel.close()
    }

    @Test
    fun `a new view model with the same store restores the state as after the death of the process`() = runTest {
        val store = testStateStore()
        val first = SearchViewModel(store)
        first.onCreated(null)
        first.dispatch(SearchAction.QueryWritten("ema"))
        advanceUntilIdle()
        first.close()

        val second = SearchViewModel(store)
        val recorder = second.record()
        second.onCreated(null)
        advanceUntilIdle()

        recorder.assertActions(EmaInitializer.EMPTY, restoreStateOf("query" to "ema"))
        recorder.assertState(SearchState(query = "ema", results = listOf("restored")))
        second.close()
    }

    @Test
    fun `a reducer of the restore of the saved state runs`() {
        SearchViewModel(testStateStore()).assertSetup()
    }

    @Test
    fun `a reducer of the restore never runs without a backup`() {
        val viewModel = object : EmaViewModel<SearchState, SearchAction, EmaEvent.EMPTY>(
            SearchState(),
            Emanate.configuration.copy(logger = null)
        ) {
            override fun setup() = source {
                reducers {
                    add<EmaRestoreState> { state.copy(results = listOf("restored")) }
                }
            }
        }

        assertFailsWith<AssertionError> { viewModel.assertSetup() }
    }

    @Test
    fun `a null value removes the saved one`() {
        val store = testStateStore()
        store.write("query", "ema")

        store.write("query", null)

        assertTrue(store.values.isEmpty())
    }
}
