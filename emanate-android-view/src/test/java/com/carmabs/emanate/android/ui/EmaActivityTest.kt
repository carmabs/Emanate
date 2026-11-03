@file:Suppress("DEPRECATION")

package com.carmabs.emanate.android.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.widget.Toolbar
import androidx.navigation.createGraph
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.fragment
import androidx.test.core.app.ApplicationProvider
import androidx.viewbinding.ViewBinding
import com.carmabs.emanate.android.HostActivity
import com.carmabs.emanate.android.NameInitializer
import com.carmabs.emanate.android.TEST_THEME
import com.carmabs.emanate.android.TestBinding
import com.carmabs.emanate.android.ViewEvent
import com.carmabs.emanate.android.ViewState
import com.carmabs.emanate.android.ViewTestViewModel
import com.carmabs.emanate.android.constants.EMA_RESULT_CODE
import com.carmabs.emanate.android.constants.EMA_RESULT_KEY
import com.carmabs.emanate.android.extension.generateViewModel
import com.carmabs.emanate.android.extension.setInitializer
import com.carmabs.emanate.android.idleMain
import com.carmabs.emanate.android.initializer.bundle.strategy.BundleSerializerStrategy
import com.carmabs.emanate.android.navigation.EmaNavControllerNavigator
import com.carmabs.emanate.core.initializer.EmaInitializerSerializer
import com.carmabs.emanate.core.navigator.EmaNavigator
import com.carmabs.emanate.core.savestate.EmaStateStore
import com.carmabs.emanate.core.view.EmaViewModelTrigger
import com.google.android.material.appbar.AppBarLayout
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

class TestActivity : EmaActivity<TestBinding, ViewState, ViewTestViewModel, ViewEvent>() {
    val states = mutableListOf<ViewState>()
    val firstExecutions = mutableListOf<Boolean>()
    val events = mutableListOf<ViewEvent>()
    var withPopAnimations = false

    override val navigator: EmaNavigator<ViewEvent>? = null
    override val initializerStrategy = BundleSerializerStrategy.serializable<NameInitializer>()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(TEST_THEME)
        super.onCreate(savedInstanceState)
    }

    override fun createViewBinding(inflater: LayoutInflater) = TestBinding(this)

    override fun provideViewModel(store: EmaStateStore) = ViewTestViewModel()

    override fun TestBinding.onState(data: ViewState) {
        states += data
        firstExecutions += isFirstNormalExecution
        text.text = data.count.toString()
    }

    override suspend fun TestBinding.onEvent(event: ViewEvent) {
        events += event
    }

    override fun overridePopTransitionAnimations() =
        if (withPopAnimations) EmaPopActivityTransitionAnimations(android.R.anim.fade_in, null) else null

    val renderedText: CharSequence get() = binding.text.text
}

/**
 * Activity whose binding is not overridden, so it uses the default event handling.
 */
class SilentActivity : EmaActivity<TestBinding, ViewState, ViewTestViewModel, ViewEvent>() {
    override val navigator: EmaNavigator<ViewEvent>? = null
    override val initializerStrategy = BundleSerializerStrategy.EMPTY

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(TEST_THEME)
        super.onCreate(savedInstanceState)
    }

    override fun createViewBinding(inflater: LayoutInflater) = TestBinding(this)
    override fun provideViewModel(store: EmaStateStore) = ViewTestViewModel()
    override fun TestBinding.onState(data: ViewState) = Unit
}

class HomeFragment : androidx.fragment.app.Fragment()
class DetailFragment : androidx.fragment.app.Fragment()

class ToolbarBinding(context: Context) : ViewBinding {
    val toolbar = Toolbar(context)
    val appBar = AppBarLayout(context).apply { addView(toolbar) }
    val containerId = View.generateViewId()
    private val rootView = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        addView(appBar)
        addView(FrameLayout(context).apply { id = containerId })
    }

    override fun getRoot(): View = rootView
}

class TestToolbarActivity : EmaToolbarActivity<ToolbarBinding, ViewState, ViewTestViewModel, ViewEvent>() {
    var fixedTitle: String? = null
    lateinit var navHost: NavHostFragment

    val navControllerNavigator = object : EmaNavControllerNavigator<ViewEvent> {
        override val navController get() = navHost.navController
        override val activity get() = this@TestToolbarActivity
        override fun navigate(event: ViewEvent) = Unit
    }

    override val navigator: EmaNavigator<ViewEvent> = navControllerNavigator

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(TEST_THEME)
        super.onCreate(savedInstanceState)
        navHost = NavHostFragment()
        supportFragmentManager.beginTransaction().add(binding.containerId, navHost).commitNow()
        navHost.navController.graph = navHost.navController.createGraph(startDestination = "home") {
            fragment<HomeFragment>("home") { label = "Home" }
            fragment<DetailFragment>("detail") { label = "Detail" }
        }
    }

    override fun createViewBinding(inflater: LayoutInflater) = ToolbarBinding(this)
    override fun provideViewModel(store: EmaStateStore) = ViewTestViewModel()
    override fun ToolbarBinding.onState(data: ViewState) = Unit
    override fun ToolbarBinding.provideToolbar() = toolbar
    override fun ToolbarBinding.provideToolbarLayout() = appBar
    override fun provideFixedToolbarTitle() = fixedTitle

    fun hide(gone: Boolean, animate: Boolean) = hideToolbar(gone, animate)
    fun show(animate: Boolean) = showToolbar(animate)
    fun title(title: String?) = setToolbarTitle(title)
    val appBarVisibility get() = toolbarLayout.visibility
    val currentToolbar get() = toolbar
    val toolbarParent get() = toolbarLayout
}

class OrphanEmaView(override val viewModel: ViewTestViewModel) :
    EmaAndroidView<ViewState, ViewTestViewModel, ViewEvent> {
    override val coroutineScope: CoroutineScope = MainScope()
    override val navigator: EmaNavigator<ViewEvent>? = null
    override val startTrigger: EmaViewModelTrigger? = null
    override val initializerSerializer: EmaInitializerSerializer? = null
    override var previousState: ViewState? = null
    override fun onState(state: ViewState) = Unit
    override suspend fun onEvent(event: ViewEvent) = Unit
    override fun onBack(result: Any?) = false
}

class LayoutEmaView(context: Context, override val viewModel: ViewTestViewModel) :
    FrameLayout(context),
    EmaAndroidView<ViewState, ViewTestViewModel, ViewEvent> {
    override val coroutineScope: CoroutineScope = MainScope()
    override val navigator: EmaNavigator<ViewEvent>? = null
    override val startTrigger: EmaViewModelTrigger? = null
    override val initializerSerializer: EmaInitializerSerializer? = null
    override var previousState: ViewState? = null
    override fun onState(state: ViewState) = Unit
    override suspend fun onEvent(event: ViewEvent) = Unit
    override fun onBack(result: Any?) = false
}

@RunWith(RobolectricTestRunner::class)
class EmaActivityTest {

    private fun launch(intent: Intent? = null) =
        Robolectric.buildActivity(TestActivity::class.java, intent).setup().also { idleMain() }

    @Test
    fun `the activity drives the ViewModel lifecycle and renders its states and events`() {
        val controller = launch()
        val activity = controller.get()
        val viewModel = activity.viewModel

        assertEquals(listOf("created", "started", "resumed"), viewModel.hooks)
        assertEquals(listOf(ViewState()), activity.states)

        viewModel.increment()
        viewModel.done()
        idleMain()

        assertEquals(listOf(ViewState(), ViewState(count = 1)), activity.states)
        assertEquals(listOf(true, false), activity.firstExecutions)
        assertEquals(ViewState(count = 1), activity.previousState)
        assertEquals("1", activity.renderedText)
        assertEquals(listOf<ViewEvent>(ViewEvent.Done), activity.events)

        controller.pause().stop()
        assertEquals(listOf("created", "started", "resumed", "paused", "stopped"), viewModel.hooks)

        viewModel.increment()
        idleMain()
        assertEquals(2, activity.states.size)

        controller.start().resume()
        idleMain()
        assertEquals(ViewState(count = 2), activity.states.last())
    }

    @Test
    fun `the initializer of the intent reaches the ViewModel`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent(context, TestActivity::class.java)
            .setInitializer(NameInitializer("ema"), BundleSerializerStrategy.serializable<NameInitializer>())

        val activity = launch(intent).get()

        assertEquals(NameInitializer("ema"), activity.viewModel.initializer)
    }

    @Test
    fun `navigating back without navigator finishes the activity with the result`() {
        val activity = launch().get()

        assertFalse(activity.navigateBack("result"))

        assertTrue(activity.isFinishing)
        val shadow = shadowOf(activity)
        assertEquals(EMA_RESULT_CODE, shadow.resultCode)
        assertEquals("\"result\"", shadow.resultIntent.getStringExtra(EMA_RESULT_KEY))
    }

    @Test
    fun `the back button navigates back and applies the pop animations`() {
        val activity = launch().get()
        activity.withPopAnimations = true

        activity.onBackPressedDispatcher.onBackPressed()

        assertTrue(activity.isFinishing)
        assertEquals(android.R.anim.fade_in, shadowOf(activity).pendingTransitionEnterAnimationResourceId)
    }

    @Suppress("DEPRECATION")
    @Test
    fun `the deprecated onBackPressed is routed through the back delegate`() {
        val activity = launch().get()
        activity.withPopAnimations = true

        activity.onBackPressed()

        assertTrue(activity.isFinishing)
        assertEquals(android.R.anim.fade_in, shadowOf(activity).pendingTransitionEnterAnimationResourceId)
    }

    @Test
    fun `navigating up without navigator is not handled`() {
        assertFalse(launch().get().onSupportNavigateUp())
    }

    @Test
    fun `events are ignored by default`() {
        val activity = Robolectric.buildActivity(SilentActivity::class.java).setup().get()
        activity.viewModel.done()
        idleMain()
        assertTrue(activity.viewModel.hooks.contains("resumed"))
    }

    @Test
    fun `the ViewModel of a view can only be created inside a FragmentActivity`() {
        val activity = Robolectric.buildActivity(HostActivity::class.java).setup().get()
        val layoutView = LayoutEmaView(activity, ViewTestViewModel())
        assertSame(layoutView.viewModel, layoutView.generateViewModel { layoutView.viewModel }.emaViewModel)

        val orphan = OrphanEmaView(ViewTestViewModel())
        assertFailsWith<IllegalAccessException> { orphan.generateViewModel { orphan.viewModel } }
    }
}

@RunWith(RobolectricTestRunner::class)
class EmaToolbarActivityTest {

    private fun launch() = Robolectric.buildActivity(TestToolbarActivity::class.java).setup().get().also { idleMain() }

    @Test
    fun `the toolbar title follows the destination label`() {
        val activity = launch()
        assertEquals("Home", activity.supportActionBar?.title)

        val detailId = activity.navHost.navController.graph.findNode("detail")!!.id
        activity.navControllerNavigator.navigateWithAction(detailId)
        idleMain()
        assertEquals("Detail", activity.supportActionBar?.title)

        assertTrue(activity.onSupportNavigateUp())
        idleMain()
        assertEquals("Home", activity.supportActionBar?.title)
    }

    @Test
    fun `a fixed title replaces the destination label`() {
        val activity = launch()
        activity.fixedTitle = "Fixed"

        activity.title(null)
        assertEquals("Fixed", activity.supportActionBar?.title)

        activity.title("Custom")
        assertEquals("Custom", activity.supportActionBar?.title)
    }

    @Test
    fun `the toolbar can be hidden and shown without animation`() {
        val activity = launch()

        activity.hide(gone = true, animate = false)
        assertEquals(View.GONE, activity.appBarVisibility)

        activity.hide(gone = false, animate = false)
        assertEquals(View.INVISIBLE, activity.appBarVisibility)

        activity.show(animate = false)
        assertEquals(View.VISIBLE, activity.appBarVisibility)
    }

    @Test
    fun `the toolbar can be hidden and shown with animation`() {
        val activity = launch()
        val looper = shadowOf(android.os.Looper.getMainLooper())

        activity.hide(gone = true, animate = true)
        looper.idleFor(Duration.ofSeconds(1))
        assertEquals(View.GONE, activity.appBarVisibility)

        activity.show(animate = true)
        looper.idleFor(Duration.ofSeconds(1))
        assertEquals(View.VISIBLE, activity.appBarVisibility)
        assertNotNull(activity.supportActionBar)
        assertSame(activity.currentToolbar.parent, activity.toolbarParent)
    }
}
