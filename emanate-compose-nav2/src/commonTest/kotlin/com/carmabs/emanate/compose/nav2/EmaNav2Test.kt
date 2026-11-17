package com.carmabs.emanate.compose.nav2

import androidx.compose.animation.fadeIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.savedstate.savedState
import com.carmabs.emanate.compose.savestate.EmaSaveStateManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class EmaNav2Test {

    private lateinit var navController: NavHostController
    private var viewModel: CounterViewModel? = null
    private var dialogViewModel: CounterViewModel? = null
    private val received = mutableListOf<CounterEvent>()
    private var saveStateSetups = 0

    private fun ComposeUiTest.setNavHost(
        initializerSupport: EmaInitializerSupport? = EmaInitializerSupport.fromRoute(RouteInitializerSerializer),
        startDestination: String = "home"
    ) {
        setContent {
            navController = rememberNavController()
            NavHost(navController, startDestination = startDestination) {
                composable("home") { BasicText("Home") }
                emaScreen(
                    screenContent = CounterScreen(),
                    viewModel = { CounterViewModel().also { viewModel = it } },
                    initializerSupport = initializerSupport,
                    saveStateManager = EmaSaveStateManager<CounterState, CounterEvent> { _, _, _ -> saveStateSetups++ },
                    transitionAnimation = EmaAnimTransition(enterTransition = { fadeIn() }),
                    onEvent = { received += it }
                )
                emaDialog(
                    screenContent = SilentScreen(),
                    viewModel = { CounterViewModel().also { dialogViewModel = it } },
                    initializerSupport = initializerSupport,
                    onEvent = {}
                )
            }
        }
        waitForIdle()
    }

    private fun ComposeUiTest.onMain(action: () -> Unit) {
        runOnUiThread(action)
        waitForIdle()
    }

    @Test
    fun `a composable screen receives the initializer of its route`() = runComposeUiTest {
        setNavHost()
        val initializer = RouteInitializer("Tom & Jerry #1? 50%/100")

        onMain { navController.navigate(CounterScreen::class.routeId, initializer, RouteInitializerSerializer) }

        onNodeWithText("Count 0").assertIsDisplayed()
        assertEquals(initializer, viewModel?.initializer)
        assertEquals(1, saveStateSetups)
    }

    @Test
    fun `a composable screen can be opened without initializer`() = runComposeUiTest {
        setNavHost(initializerSupport = null)

        onMain { navController.navigate(CounterScreen::class.routeId) }

        onNodeWithText("Count 0").assertIsDisplayed()
        assertNull(viewModel?.initializer)
    }

    @Test
    fun `the origin of the support is used when the route has no initializer`() = runComposeUiTest {
        setNavHost(EmaInitializerSupport.fromInitializer(RouteInitializerSerializer, RouteInitializer("backup")))

        onMain { navController.navigate(CounterScreen::class.routeId) }

        assertEquals(RouteInitializer("backup"), viewModel?.initializer)
    }

    @Test
    fun `the initializer of the route wins over the origin of the support`() = runComposeUiTest {
        setNavHost(EmaInitializerSupport.fromInitializer(RouteInitializerSerializer, RouteInitializer("backup")))

        onMain {
            navController.navigate(CounterScreen::class.routeId, RouteInitializer("route"), RouteInitializerSerializer)
        }

        assertEquals(RouteInitializer("route"), viewModel?.initializer)
    }

    @Test
    fun `an initializer without values in the route avoids the origin of the support`() = runComposeUiTest {
        setNavHost(EmaInitializerSupport.fromInitializer(RouteInitializerSerializer, RouteInitializer("backup")))

        onMain {
            navController.navigate(CounterScreen::class.routeId, RouteInitializer(""), RouteInitializerSerializer)
        }

        assertEquals(RouteInitializer(""), viewModel?.initializer)
    }

    @Test
    fun `a screen can be shown as a dialog`() = runComposeUiTest {
        setNavHost()

        onMain { navController.navigate(SilentScreen::class.routeId) { launchSingleTop = true } }

        onNodeWithText("Silent 0").assertIsDisplayed()
    }

    @Test
    fun `a dialog receives the initializer of its route`() = runComposeUiTest {
        setNavHost()
        val initializer = RouteInitializer("dialog")

        onMain { navController.navigate(SilentScreen::class.routeId, initializer, RouteInitializerSerializer) }

        onNodeWithText("Silent 0").assertIsDisplayed()
        assertEquals(initializer, dialogViewModel?.initializer)
    }

    @Test
    fun `navigating back pops the screens and reports the empty back stack`() = runComposeUiTest {
        setNavHost()
        onMain { navController.navigate(CounterScreen::class.routeId) }

        var popped = false
        var emptied = 0
        onMain { popped = navController.navigateBack { emptied++ } }
        assertTrue(popped)
        assertEquals(0, emptied)
        onNodeWithText("Home").assertIsDisplayed()

        onMain { popped = navController.navigateBack { emptied++ } }
        assertFalse(popped)
        assertEquals(1, emptied)
    }

    @Test
    fun `the routes include the serialized initializer`() {
        assertEquals("route", routeWithInitializer("route", null, RouteInitializerSerializer))
        assertTrue(
            routeWithInitializer("route", RouteInitializer("ema"), RouteInitializerSerializer)
                .startsWith("route?")
        )
        assertEquals("screen/${CounterScreen::class.qualifiedName}", CounterScreen::class.routeId)
    }

    @Test
    fun `the initializer nav type stores the initializer in the saved state and in the route`() {
        val navType = EmaInitializerNavType(RouteInitializerSerializer)
        val bundle = savedState()

        navType.put(bundle, "key", RouteInitializer("ema"))

        assertEquals(RouteInitializer("ema"), navType.get(bundle, "key"))
        assertEquals(RouteInitializer("ema"), navType.parseValue(navType.serializeAsValue(RouteInitializer("ema"))))
    }

    @Test
    fun `the pop transitions default to the enter and exit transitions`() {
        val empty = EmaAnimTransition()
        assertNull(empty.enterTransition)
        assertNull(empty.popExitTransition)

        val transitions = EmaAnimTransition(enterTransition = { fadeIn() }, exitTransition = { null })
        assertSame(transitions.enterTransition, transitions.popEnterTransition)
        assertSame(transitions.exitTransition, transitions.popExitTransition)
        assertNotNull(EmaInitializerSupport.fromRoute(RouteInitializerSerializer).serializer)
    }
}
