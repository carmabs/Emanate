@file:Suppress("DEPRECATION")

package com.carmabs.emanate.android.navigation

import androidx.fragment.app.Fragment
import androidx.navigation.createGraph
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.fragment
import com.carmabs.emanate.android.HostActivity
import com.carmabs.emanate.android.ViewEvent
import com.carmabs.emanate.android.constants.EMA_RESULT_CODE
import com.carmabs.emanate.android.constants.EMA_RESULT_KEY
import com.carmabs.emanate.android.idleMain
import com.carmabs.emanate.core.state.EmaEvent
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

class FirstFragment : Fragment()
class SecondFragment : Fragment()

@RunWith(RobolectricTestRunner::class)
class EmaNavigatorTest {

    private val activity = Robolectric.buildActivity(HostActivity::class.java).setup().get()

    private val navHost = NavHostFragment().also { navHost ->
        activity.supportFragmentManager.beginTransaction().add(activity.containerId, navHost).commitNow()
        navHost.navController.graph = navHost.navController.createGraph(startDestination = "first") {
            fragment<FirstFragment>("first")
            fragment<SecondFragment>("second")
        }
        idleMain()
    }

    private val navController get() = navHost.navController

    private fun goToSecond() {
        navController.navigate("second")
        idleMain()
    }

    private fun assertFinishedWithResult(result: String) {
        assertTrue(activity.isFinishing)
        assertEquals(EMA_RESULT_CODE, shadowOf(activity).resultCode)
        assertEquals(result, shadowOf(activity).resultIntent.getStringExtra(EMA_RESULT_KEY))
    }

    @Test
    fun `the empty navigator pops the back stack and finishes at its root`() {
        val navigator = EmaEmptyNavigator(activity, navController)
        navigator.navigate(EmaEmptyNavigationEvent)
        goToSecond()

        assertTrue(navigator.navigateBack())
        assertFalse(activity.isFinishing)

        assertFalse(navigator.navigateBack("done"))
        assertFinishedWithResult("\"done\"")
    }

    @Test
    fun `the empty navigator finishes without result`() {
        assertFalse(EmaEmptyNavigator(activity, navController).navigateBack())
        assertTrue(activity.isFinishing)
        assertEquals(null, shadowOf(activity).resultIntent)
    }

    @Test
    fun `the activity host navigates back through its fragments and finishes at its root`() {
        val host = EmaActivityNavControllerHost(activity, activity.containerId, graphId = 0)
        host.navigate(EmaEvent.EMPTY)
        assertSame(navController, host.navController)
        goToSecond()

        assertTrue(host.navigateBack("ignored"))
        assertFalse(activity.isFinishing)

        assertFalse(host.navigateBack(listOf(1, 2)))
        assertFinishedWithResult("[1,2]")
    }

    @Test
    fun `the activity host finishes without result`() {
        assertFalse(EmaActivityNavControllerHost(activity, activity.containerId, graphId = 0).navigateBack())
        assertTrue(activity.isFinishing)
    }

    @Test
    fun `the fragment navigator uses the NavController of the fragment`() {
        val fragment = navHost.childFragmentManager.primaryNavigationFragment!!
        val navigator = object : EmaFragmentNavControllerNavigator<ViewEvent>(fragment) {
            override fun navigate(event: ViewEvent) {
                navigateWithAction(navController.graph.findNode("second")!!.id)
            }
        }

        assertSame(activity, navigator.activity)
        assertSame(navController, navigator.navController)

        navigator.navigate(ViewEvent.Done)
        idleMain()
        assertEquals("second", navController.currentDestination?.route)

        assertTrue(navigator.navigateBack())
        assertFalse(navigator.navigateBack())
    }

    @Test
    fun `the activity navigator finds the NavController of its host fragment`() {
        val navigator = object : EmaActivityNavControllerNavigator<ViewEvent>(activity, activity.containerId, 0) {
            override fun navigate(event: ViewEvent) = Unit
        }

        assertSame(navController, navigator.navController)
        assertSame(activity, navigator.activity)
    }
}
