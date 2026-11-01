package com.carmabs.emanate.compose.navigation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.carmabs.emanate.android.initializer.EmaInitializerBundle
import com.carmabs.emanate.android.initializer.bundle.strategy.BundleSerializerStrategy
import com.carmabs.emanate.android.savestate.EmaSaveStateManager
import com.carmabs.emanate.compose.CounterEvent
import com.carmabs.emanate.compose.CounterScreen
import com.carmabs.emanate.compose.CounterState
import com.carmabs.emanate.compose.CounterViewModel
import com.carmabs.emanate.compose.RouteInitializer
import com.carmabs.emanate.compose.RouteInitializerSerializer
import com.carmabs.emanate.compose.SilentScreen
import com.carmabs.emanate.compose.extension.emaDialog
import com.carmabs.emanate.compose.extension.emaScreen
import com.carmabs.emanate.compose.extension.navigate
import com.carmabs.emanate.compose.extension.navigateBack
import com.carmabs.emanate.compose.extension.navigateToExternalLink
import com.carmabs.emanate.compose.extension.routeId
import com.carmabs.emanate.compose.extension.routeWithInitializer
import com.carmabs.emanate.compose.initializer.EmaInitializerSupport
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class EmaComposeNavigationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val strategy = BundleSerializerStrategy.kSerialization(RouteInitializerSerializer)
    private val noInitializer: EmaInitializerBundle? = null

    private lateinit var navController: NavHostController
    private var viewModel: CounterViewModel? = null
    private var dialogViewModel: CounterViewModel? = null
    private val received = mutableListOf<CounterEvent>()
    private var saveStateSetups = 0

    private fun setNavHost(
        initializerSupport: EmaInitializerSupport? = EmaInitializerSupport.fromRoute(RouteInitializerSerializer),
        startDestination: String = "home"
    ) {
        composeRule.setContent {
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
        composeRule.waitForIdle()
    }

    private fun onMain(action: () -> Unit) {
        composeRule.runOnUiThread(action)
        composeRule.waitForIdle()
    }

    @Test
    fun `a composable screen receives the initializer of its route`() {
        setNavHost()
        val initializer = RouteInitializer("Tom & Jerry #1? 50%/100")

        onMain { navController.navigate(CounterScreen::class.routeId, EmaInitializerBundle(initializer, strategy)) }

        composeRule.onNodeWithText("Count 0").assertIsDisplayed()
        assertEquals(initializer, viewModel?.initializer)
        assertEquals(1, saveStateSetups)
    }

    @Test
    fun `a composable screen can be opened without initializer`() {
        setNavHost(initializerSupport = null)

        onMain { navController.navigate(CounterScreen::class.routeId, initializerBundle = noInitializer) }

        composeRule.onNodeWithText("Count 0").assertIsDisplayed()
        assertNull(viewModel?.initializer)
    }

    @Test
    fun `the origin of the support is used when the route has no initializer`() {
        setNavHost(EmaInitializerSupport.fromInitializer(RouteInitializerSerializer, RouteInitializer("backup")))

        onMain { navController.navigate(CounterScreen::class.routeId, initializerBundle = noInitializer) }

        assertEquals(RouteInitializer("backup"), viewModel?.initializer)
    }

    @Test
    fun `the initializer of the route wins over the origin of the support`() {
        setNavHost(EmaInitializerSupport.fromInitializer(RouteInitializerSerializer, RouteInitializer("backup")))

        onMain {
            navController.navigate(
                CounterScreen::class.routeId,
                EmaInitializerBundle(RouteInitializer("route"), strategy)
            )
        }

        assertEquals(RouteInitializer("route"), viewModel?.initializer)
    }

    @Test
    fun `an initializer without values in the route avoids the origin of the support`() {
        setNavHost(EmaInitializerSupport.fromInitializer(RouteInitializerSerializer, RouteInitializer("backup")))

        onMain {
            navController.navigate(
                CounterScreen::class.routeId,
                EmaInitializerBundle(RouteInitializer(""), strategy)
            )
        }

        assertEquals(RouteInitializer(""), viewModel?.initializer)
    }

    @Test
    fun `the first screen of an activity reads the initializer of its intent`() {
        composeRule.activity.intent.putExtras(Bundle().also {
            strategy.save(RouteInitializer("intent"), it)
        })

        setNavHost(
            initializerSupport = EmaInitializerSupport.fromIntent(RouteInitializerSerializer),
            startDestination = CounterScreen::class.routeId
        )

        composeRule.onNodeWithText("Count 0").assertIsDisplayed()
        assertEquals(RouteInitializer("intent"), viewModel?.initializer)
    }

    @Test
    fun `a screen reads the initializer of a bundle`() {
        val bundle = Bundle().also { strategy.save(RouteInitializer("bundle"), it) }
        setNavHost(EmaInitializerSupport.fromBundle(RouteInitializerSerializer, bundle))

        onMain { navController.navigate(CounterScreen::class.routeId, initializerBundle = noInitializer) }

        assertEquals(RouteInitializer("bundle"), viewModel?.initializer)
    }

    @Test
    fun `a screen can be shown as a dialog`() {
        setNavHost()

        onMain { navController.navigate(SilentScreen::class.routeId, noInitializer) { launchSingleTop = true } }

        composeRule.onNodeWithText("Silent 0").assertIsDisplayed()
    }

    @Test
    fun `a dialog receives the initializer of its route`() {
        setNavHost()
        val initializer = RouteInitializer("dialog")

        onMain { navController.navigate(SilentScreen::class.routeId, EmaInitializerBundle(initializer, strategy)) }

        composeRule.onNodeWithText("Silent 0").assertIsDisplayed()
        assertEquals(initializer, dialogViewModel?.initializer)
    }

    @Test
    fun `navigating back pops the screens and finishes the activity at the root`() {
        setNavHost()
        onMain { navController.navigate(CounterScreen::class.routeId, initializerBundle = noInitializer) }

        var popped = false
        onMain { popped = navController.navigateBack() }
        assertTrue(popped)
        composeRule.onNodeWithText("Home").assertIsDisplayed()

        onMain { popped = navController.navigateBack(closeActivityWhenBackstackIsEmpty = false) }
        assertFalse(popped)
        assertFalse(composeRule.activity.isFinishing)

        onMain { popped = navController.navigateBack() }
        assertFalse(popped)
        assertTrue(composeRule.activity.isFinishing)
    }

    @Test
    fun `external links are opened in another activity`() {
        setNavHost()
        var opened = false

        onMain { opened = navController.navigateToExternalLink("https://github.com/carmabs") }

        assertTrue(opened)
        val intent = shadowOf(composeRule.activity).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals("https://github.com/carmabs", intent.dataString)
    }

    @Test
    fun `the routes include the serialized initializer`() {
        assertEquals("route", routeWithInitializer("route", null))
        assertTrue(
            routeWithInitializer("route", EmaInitializerBundle(RouteInitializer("ema"), strategy))
                .startsWith("route?")
        )
        assertEquals("screen/${CounterScreen::class.java.name}", CounterScreen::class.routeId)
    }

    @Test
    fun `the initializer nav type stores the initializer in the bundle and in the route`() {
        val navType = EmaInitializerNavType(strategy)
        val bundle = Bundle()

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
        assertNotNull(EmaInitializerSupport.fromRoute(RouteInitializerSerializer).serializerStrategy)
    }
}
