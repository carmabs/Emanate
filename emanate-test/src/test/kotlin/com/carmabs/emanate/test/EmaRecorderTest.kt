package com.carmabs.emanate.test

import com.carmabs.emanate.core.Emanate
import com.carmabs.emanate.core.action.EmaAction
import com.carmabs.emanate.core.broadcast.EmaActionBroadcaster
import com.carmabs.emanate.core.model.EmaConfiguration
import com.carmabs.emanate.core.source.EmaSource
import com.carmabs.emanate.core.state.EmaEvent
import com.carmabs.emanate.core.state.EmaState
import com.carmabs.emanate.core.viewmodel.EmaLifecycle
import com.carmabs.emanate.core.viewmodel.EmaViewModel
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

private data class ProfileState(val name: String = "", val loading: Boolean = false) : EmaState

private sealed interface ProfileAction : EmaAction {
    data object Load : ProfileAction
    data class Loaded(val name: String) : ProfileAction
}

private sealed interface ProfileEvent : EmaEvent {
    data object Welcome : ProfileEvent
}

private class ProfileViewModel(configuration: EmaConfiguration = Emanate.configuration) :
    EmaViewModel<ProfileState, ProfileAction, ProfileEvent>(
        initialDataState = ProfileState(),
        configuration = configuration.copy(logger = null)
    ) {
    override fun setup() = source {
        reducers {
            add<ProfileAction> { action ->
                when (action) {
                    ProfileAction.Load -> state.copy(loading = true)
                    is ProfileAction.Loaded -> state.copy(name = action.name, loading = false)
                }
            }
        }
        sideEffects {
            add<ProfileAction.Load> {
                delay(1000)
                dispatch(ProfileAction.Loaded("Ema"))
            }
            add<ProfileAction.Loaded> { publish(ProfileEvent.Welcome) }
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class EmaRecorderTest {

    @BeforeTest
    fun setUp() {
        useTestConfiguration()
    }

    @AfterTest
    fun tearDown() {
        resetTestConfiguration()
    }

    @Test
    fun `records a view model`() = runTest {
        val viewModel = ProfileViewModel()
        val recorder = viewModel.record()

        viewModel.dispatch(ProfileAction.Load)
        advanceUntilIdle()

        recorder.assertActions(ProfileAction.Load, ProfileAction.Loaded("Ema"))
        recorder.assertStates(ProfileState(loading = true), ProfileState(name = "Ema"))
        recorder.assertState(ProfileState(name = "Ema"))
        recorder.assertState { !it.loading }
        recorder.assertEvents(ProfileEvent.Welcome)
    }

    @Test
    fun `records a source`() = runTest {
        val source = EmaSource<ProfileState, ProfileAction, ProfileEvent>(ProfileState()) {
            reducers {
                add<ProfileAction> { action ->
                    when (action) {
                        ProfileAction.Load -> state.copy(loading = true)
                        is ProfileAction.Loaded -> state.copy(name = action.name, loading = false)
                    }
                }
            }
        }
        val recorder = source.record()

        source.dispatch(ProfileAction.Loaded("Ema"))
        advanceUntilIdle()

        recorder.assertActions(ProfileAction.Loaded("Ema"))
        recorder.assertState(ProfileState(name = "Ema"))
        recorder.assertNoEvents()
        source.close()
    }

    @Test
    fun `failed checks explain the difference`() = runTest {
        val viewModel = ProfileViewModel()
        val recorder = viewModel.record()

        viewModel.dispatch(ProfileAction.Load)

        val error = assertFailsWith<AssertionError> { recorder.assertActions(ProfileAction.Loaded("Ema")) }
        assertEquals("Expected actions <[Loaded(name=Ema)]> but were <[Load]>", error.message)
        assertFailsWith<AssertionError> { recorder.assertState { !it.loading } }
        assertFailsWith<AssertionError> { recorder.assertEvents(ProfileEvent.Welcome) }
    }

    @Test
    fun `the view model can be cleared in the test`() = runTest {
        val viewModel = ProfileViewModel()
        val recorder = viewModel.record()

        viewModel.onResumeView()
        viewModel.close()
        advanceUntilIdle()

        recorder.assertActions(EmaLifecycle.Resumed, EmaLifecycle.Destroyed)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class EmaRecorderStandardDispatcherTest {

    private lateinit var dispatcher: TestDispatcher

    @BeforeTest
    fun setUp() {
        dispatcher = useTestConfiguration(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        resetTestConfiguration()
    }

    @Test
    fun `with a standard dispatcher the actions run when the test advances`() = runTest {
        val viewModel = ProfileViewModel()
        val recorder = viewModel.record()

        viewModel.dispatch(ProfileAction.Load)
        recorder.assertActions()

        advanceUntilIdle()
        recorder.assertActions(ProfileAction.Load, ProfileAction.Loaded("Ema"))
    }

    @Test
    fun `resetting the dispatchers restores the configuration of Emanate`() {
        assertSame(dispatcher, Emanate.configuration.dispatchers.main)

        resetTestConfiguration()

        assertNotSame(dispatcher, Emanate.configuration.dispatchers.main)
    }
}

/**
 * Without useTestConfiguration: the view models receive the configuration of the test
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TestConfigurationTest {

    @Test
    fun `a view model with the test configuration runs on the scheduler of the test`() = runTest {
        val viewModel = ProfileViewModel(testConfiguration(UnconfinedTestDispatcher(testScheduler)))
        val recorder = viewModel.record()

        viewModel.dispatch(ProfileAction.Load)
        recorder.assertActions(ProfileAction.Load)

        advanceUntilIdle()
        recorder.assertActions(ProfileAction.Load, ProfileAction.Loaded("Ema"))
        recorder.assertEvents(ProfileEvent.Welcome)
    }

    @Test
    fun `with a standard dispatcher the actions run when the test advances`() = runTest {
        val viewModel = ProfileViewModel(testConfiguration(StandardTestDispatcher(testScheduler)))
        val recorder = viewModel.record()

        viewModel.dispatch(ProfileAction.Load)
        recorder.assertActions()

        advanceUntilIdle()
        recorder.assertActions(ProfileAction.Load, ProfileAction.Loaded("Ema"))
    }

    @Test
    fun `the test configuration doesn't change the configuration of Emanate`() {
        val dispatcher = StandardTestDispatcher()
        val configuration = testConfiguration(dispatcher, EmaConfiguration(logger = null))

        assertSame(dispatcher, configuration.dispatchers.io)
        assertEquals(null, configuration.logger)
        assertNotSame(dispatcher, Emanate.configuration.dispatchers.main)
    }

    @Test
    fun `each test configuration has its own broadcaster unless it is given one`() {
        val broadcaster = EmaActionBroadcaster()
        val dispatcher = StandardTestDispatcher()

        assertNotSame(EmaActionBroadcaster.Shared, testConfiguration(dispatcher).broadcaster)
        assertNotSame(testConfiguration(dispatcher).broadcaster, testConfiguration(dispatcher).broadcaster)
        assertSame(broadcaster, testConfiguration(dispatcher, EmaConfiguration(broadcaster = broadcaster)).broadcaster)
    }
}
