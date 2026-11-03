@file:Suppress("DEPRECATION")

package com.carmabs.emanate.android.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import androidx.navigation.fragment.NavHostFragment
import androidx.test.core.app.ApplicationProvider
import com.carmabs.emanate.android.NameInitializer
import com.carmabs.emanate.android.TEST_THEME
import com.carmabs.emanate.android.ViewEvent
import com.carmabs.emanate.android.ViewState
import com.carmabs.emanate.android.ViewTestViewModel
import com.carmabs.emanate.android.extension.getInitializer
import com.carmabs.emanate.android.extension.setInitializer
import com.carmabs.emanate.android.idleMain
import com.carmabs.emanate.android.initializer.bundle.strategy.BundleSerializerStrategy
import com.carmabs.emanate.android.navigation.EmaActivityNavControllerNavigator
import com.carmabs.emanate.android.view.test.R
import com.carmabs.emanate.core.initializer.EmaInitializer
import com.carmabs.emanate.core.savestate.EmaStateStore
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

class GraphActivity : EmaActivity<ToolbarBinding, ViewState, ViewTestViewModel, ViewEvent>() {

    companion object {
        var destinationInitializer: EmaInitializer? = null
    }

    override val initializerStrategy = BundleSerializerStrategy.serializable<NameInitializer>()

    val graphNavigator by lazy {
        object : EmaActivityNavControllerNavigator<ViewEvent>(this, binding.containerId, R.xml.test_graph) {
            override fun navigate(event: ViewEvent) = navigateWithAction(R.id.detailFragment)
        }
    }

    override val navigator get() = graphNavigator

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(TEST_THEME)
        super.onCreate(savedInstanceState)
        supportFragmentManager.beginTransaction().add(binding.containerId, NavHostFragment()).commitNow()
    }

    override fun overrideDestinationInitializer() = destinationInitializer

    override fun createViewBinding(inflater: LayoutInflater) = ToolbarBinding(this)
    override fun provideViewModel(store: EmaStateStore) = ViewTestViewModel()
    override fun ToolbarBinding.onState(data: ViewState) = Unit
}

@RunWith(RobolectricTestRunner::class)
class EmaNavGraphActivityTest {

    private val strategy = BundleSerializerStrategy.serializable<NameInitializer>()

    @After
    fun resetInitializer() {
        GraphActivity.destinationInitializer = null
    }

    private fun launch(initializer: NameInitializer? = null): GraphActivity {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent(context, GraphActivity::class.java).apply {
            initializer?.also { setInitializer(it, strategy) }
        }
        return Robolectric.buildActivity(GraphActivity::class.java, intent).setup().get().also { idleMain() }
    }

    private val GraphActivity.startArguments
        get() = graphNavigator.navController.currentBackStackEntry?.arguments

    @Test
    fun `the graph receives the initializer of the activity`() {
        val activity = launch(NameInitializer("intent"))

        assertEquals(R.id.homeFragment, activity.graphNavigator.navController.currentDestination?.id)
        assertEquals(NameInitializer("intent"), activity.startArguments?.getInitializer<NameInitializer>(strategy))
    }

    @Test
    fun `the activity can override the initializer of the start destination`() {
        GraphActivity.destinationInitializer = NameInitializer("override")

        val activity = launch(NameInitializer("intent"))

        assertEquals(NameInitializer("override"), activity.startArguments?.getInitializer<NameInitializer>(strategy))
    }

    @Test
    fun `the activity navigates through its graph and up`() {
        val activity = launch()

        activity.navigate(ViewEvent.Done)
        idleMain()
        assertEquals(R.id.detailFragment, activity.graphNavigator.navController.currentDestination?.id)

        assertTrue(activity.onSupportNavigateUp())
        idleMain()
        assertEquals(R.id.homeFragment, activity.graphNavigator.navController.currentDestination?.id)
        assertFalse(activity.onSupportNavigateUp())
    }
}
