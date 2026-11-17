package com.carmabs.emanate.compose.nav2

import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventInput
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A system back, as the platforms send it to the screens.
 */
private class BackInput : NavigationEventInput() {
    fun back() = dispatchOnBackCompleted()
}

@OptIn(ExperimentalTestApi::class)
class EmaBackHandlingTest {

    private lateinit var navController: NavHostController
    private var dispatcher: NavigationEventDispatcher? = null
    private var viewModel: CounterViewModel? = null

    private fun ComposeUiTest.openCounter(backEnabled: Boolean) {
        setContent {
            dispatcher = LocalNavigationEventDispatcherOwner.current?.navigationEventDispatcher
            navController = rememberNavController()
            NavHost(navController, startDestination = "home") {
                composable("home") { BasicText("Home") }
                emaScreen(
                    screenContent = CounterScreen(backEnabled),
                    viewModel = { CounterViewModel().also { viewModel = it } },
                    onEvent = {}
                )
            }
        }
        runOnUiThread { navController.navigate(CounterScreen::class.routeId) }
        waitForIdle()
    }

    private fun ComposeUiTest.sendBack() {
        val input = BackInput()
        runOnUiThread {
            dispatcher?.addInput(input)
            input.back()
        }
        waitForIdle()
    }

    @Test
    fun `a screen that intercepts the back receives its action and stays`() = runComposeUiTest {
        openCounter(backEnabled = true)

        sendBack()

        assertTrue("back" in viewModel!!.hooks)
        assertEquals(
            CounterScreen::class.routeId,
            navController.currentBackStackEntry?.destination?.route?.substringBefore('?')
        )
    }

    @Test
    fun `a screen that does not intercept the back lets the navigation pop it`() = runComposeUiTest {
        openCounter(backEnabled = false)

        sendBack()

        assertEquals("home", navController.currentBackStackEntry?.destination?.route)
    }
}
