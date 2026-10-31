package com.carmabs.emanate.compose.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import com.carmabs.emanate.android.savestate.EmaSaveStateManager
import com.carmabs.emanate.android.savestate.SavedStateSupport
import com.carmabs.emanate.compose.ClassicViewModel
import com.carmabs.emanate.compose.CounterEvent
import com.carmabs.emanate.compose.CounterScreen
import com.carmabs.emanate.compose.CounterState
import com.carmabs.emanate.compose.CounterViewModel
import com.carmabs.emanate.compose.RouteInitializer
import com.carmabs.emanate.compose.SilentScreen
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EmaScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun pressBack() = composeRule.runOnUiThread {
        composeRule.activity.onBackPressedDispatcher.onBackPressed()
    }

    @Test
    fun `the screen renders the states, dispatches the actions and receives the events`() {
        lateinit var viewModel: CounterViewModel
        val received = mutableListOf<CounterEvent>()
        composeRule.setContent {
            EmaScreen(
                initializer = RouteInitializer("screen"),
                vm = { CounterViewModel().also { viewModel = it } },
                screenContent = CounterScreen(),
                onEvent = { received += it }
            )
        }

        composeRule.onNodeWithText("Count 0").assertIsDisplayed()
        composeRule.onNodeWithText("Increment").performClick()
        composeRule.onNodeWithText("Count 1").assertIsDisplayed()

        composeRule.onNodeWithText("Notify").performClick()
        composeRule.waitForIdle()

        assertEquals(listOf<CounterEvent>(CounterEvent.Notified), received)
        assertEquals(listOf("created", "started", "resumed"), viewModel.hooks)
        assertEquals(RouteInitializer("screen"), viewModel.initializer)
    }

    @Test
    fun `the lifecycle of the host reaches the ViewModel`() {
        lateinit var viewModel: CounterViewModel
        composeRule.setContent {
            EmaScreen(vm = {
                CounterViewModel().also { viewModel = it }
            }, screenContent = CounterScreen(), onEvent = {})
        }
        composeRule.waitForIdle()

        composeRule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)

        assertEquals(listOf("created", "started", "resumed", "paused", "stopped"), viewModel.hooks)
    }

    @Test
    fun `leaving the composition pauses and stops the ViewModel`() {
        lateinit var viewModel: CounterViewModel
        val show = mutableStateOf(true)
        composeRule.setContent {
            if (show.value) {
                EmaScreen(vm = {
                    CounterViewModel().also { viewModel = it }
                }, screenContent = CounterScreen(), onEvent = {})
            }
        }
        composeRule.waitForIdle()

        show.value = false
        composeRule.waitForIdle()

        assertEquals(listOf("paused", "stopped"), viewModel.hooks.takeLast(2))
    }

    @Test
    fun `the back action of the screen is dispatched to the ViewModel`() {
        lateinit var viewModel: CounterViewModel
        composeRule.setContent {
            EmaScreen(vm = {
                CounterViewModel().also { viewModel = it }
            }, screenContent = CounterScreen(), onEvent = {})
        }
        composeRule.waitForIdle()

        pressBack()

        assertTrue(viewModel.hooks.contains("back"))
        assertFalse(composeRule.activity.isFinishing)
    }

    @Test
    fun `without back action the system handles the back press`() {
        composeRule.setContent {
            EmaScreen(vm = {
                CounterViewModel()
            }, screenContent = CounterScreen(backEnabled = false), onEvent = {})
        }
        composeRule.waitForIdle()

        pressBack()

        assertTrue(composeRule.activity.isFinishing)
    }

    @Test
    fun `a ViewModel instance can be rendered with its own action dispatcher`() {
        val viewModel = CounterViewModel()
        composeRule.setContent {
            EmaScreen(vm = viewModel, actions = viewModel, screenContent = SilentScreen(), onEvent = {})
        }

        composeRule.onNodeWithText("Silent 0").performClick()
        composeRule.waitForIdle()

        assertEquals(listOf("created", "started", "resumed"), viewModel.hooks)
        pressBack()
        assertTrue(composeRule.activity.isFinishing)
    }

    @Test
    fun `the save state manager is set up once`() {
        var setups = 0
        val support = SavedStateSupport(
            SavedStateHandle(),
            EmaSaveStateManager<CounterState, CounterEvent> { _, _, _ -> setups++ }
        )
        val recomposition = mutableIntStateOf(0)
        composeRule.setContent {
            EmaScreen(
                vm = { CounterViewModel() },
                screenContent = CounterScreen(),
                onEvent = {},
                saveStateSupport = support,
                previewRenderState = CounterState(recomposition.intValue)
            )
        }
        composeRule.waitForIdle()

        recomposition.intValue = 1
        composeRule.waitForIdle()
        recomposition.intValue = 2
        composeRule.waitForIdle()

        assertEquals(1, setups)
    }

    @Test
    fun `in a preview the screen renders the preview state without ViewModel`() {
        composeRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                EmaScreen(
                    vm = { error("The ViewModel is not created in previews") },
                    screenContent = CounterScreen(),
                    onEvent = { _: CounterEvent -> },
                    previewRenderState = CounterState(5)
                )
                EmaScreen(
                    vm = ClassicViewModel(),
                    actions = CounterViewModel(),
                    screenContent = SilentScreen(),
                    onEvent = {},
                    previewRenderState = CounterState(7)
                )
                EmaScreen(vm = { CounterViewModel() }, screenContent = SilentScreen(), onEvent = {})
                EmaScreen(
                    vm = ClassicViewModel(),
                    actions = CounterViewModel(),
                    screenContent = SilentScreen(),
                    onEvent = {}
                )
            }
        }

        composeRule.onNodeWithText("Count 5").assertIsDisplayed()
        composeRule.onNodeWithText("Silent 7").assertIsDisplayed()
        composeRule.onNodeWithText("Increment").performClick()
    }
}
