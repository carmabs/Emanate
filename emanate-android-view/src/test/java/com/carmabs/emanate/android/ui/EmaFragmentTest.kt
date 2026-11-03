@file:Suppress("DEPRECATION")

package com.carmabs.emanate.android.ui

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.createGraph
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.fragment
import androidx.test.core.app.ApplicationProvider
import com.carmabs.emanate.android.DelegateNotOwnerActivity
import com.carmabs.emanate.android.DelegateOwnerActivity
import com.carmabs.emanate.android.HostActivity
import com.carmabs.emanate.android.NameInitializer
import com.carmabs.emanate.android.TestBinding
import com.carmabs.emanate.android.ViewEvent
import com.carmabs.emanate.android.ViewState
import com.carmabs.emanate.android.ViewTestViewModel
import com.carmabs.emanate.android.constants.EMA_RESULT_CODE
import com.carmabs.emanate.android.constants.EMA_RESULT_KEY
import com.carmabs.emanate.android.extension.getInitializer
import com.carmabs.emanate.android.extension.setEmaResultListener
import com.carmabs.emanate.android.extension.setInitializer
import com.carmabs.emanate.android.idleMain
import com.carmabs.emanate.android.initializer.bundle.strategy.BundleSerializerStrategy
import com.carmabs.emanate.android.permission.EmaAndroidPermissionManager
import com.carmabs.emanate.core.manager.PermissionState
import com.carmabs.emanate.core.navigator.EmaNavigator
import com.carmabs.emanate.core.savestate.EmaStateStore
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

class TestFragment : EmaFragment<TestBinding, ViewState, ViewTestViewModel, ViewEvent>() {
    val states = mutableListOf<ViewState>()
    val firstExecutions = mutableListOf<Boolean>()
    val events = mutableListOf<ViewEvent>()
    val results = mutableListOf<String>()
    var activityScope = false
    var manualBack = false
    var listenResults = false

    val permissionManager = EmaAndroidPermissionManager(this)

    override val navigator: EmaNavigator<ViewEvent>? = null
    override val fragmentViewModelScope get() = !activityScope
    override val handleBackPressedManually get() = manualBack
    override val initializerStrategy get() = BundleSerializerStrategy.serializable<NameInitializer>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (listenResults) setEmaResultListener<String> { results += it }
    }

    override fun createViewBinding(inflater: LayoutInflater, container: ViewGroup?) = TestBinding(inflater.context)

    override fun provideViewModel(store: EmaStateStore) = ViewTestViewModel()

    override fun TestBinding.onState(state: ViewState) {
        states += state
        firstExecutions += isFirstNormalExecution
        text.text = state.count.toString()
    }

    override suspend fun TestBinding.onEvent(event: ViewEvent) {
        events += event
    }

    fun scope() = getScope()
    val renderedText: CharSequence get() = binding.text.text
}

class SilentFragment : EmaFragment<TestBinding, ViewState, ViewTestViewModel, ViewEvent>() {
    override val navigator: EmaNavigator<ViewEvent>? = null
    override fun createViewBinding(inflater: LayoutInflater, container: ViewGroup?) = TestBinding(inflater.context)
    override fun provideViewModel(store: EmaStateStore) = ViewTestViewModel()
    override fun TestBinding.onState(state: ViewState) = Unit
}

class StartFragment : androidx.fragment.app.Fragment()

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class EmaFragmentTest {

    private fun <A : HostActivity> host(activityClass: Class<A>) =
        Robolectric.buildActivity(activityClass).setup().get()

    private fun HostActivity.add(fragment: androidx.fragment.app.Fragment, backStack: Boolean = false) {
        supportFragmentManager.beginTransaction().add(containerId, fragment).apply {
            if (backStack) addToBackStack(null)
        }.commit()
        supportFragmentManager.executePendingTransactions()
        idleMain()
    }

    @Test
    fun `the fragment drives the ViewModel lifecycle and renders its states and events`() {
        val activity = host(HostActivity::class.java)
        val fragment = TestFragment()
        activity.add(fragment)
        val viewModel = fragment.viewModel

        assertEquals(listOf("created", "started", "resumed"), viewModel.hooks)
        viewModel.increment()
        viewModel.done()
        idleMain()

        assertEquals(listOf(ViewState(), ViewState(count = 1)), fragment.states)
        assertEquals(listOf(true, false), fragment.firstExecutions)
        assertEquals("1", fragment.renderedText)
        assertEquals(listOf<ViewEvent>(ViewEvent.Done), fragment.events)
        assertSame(fragment.viewLifecycleOwner.lifecycleScope, fragment.viewScope)

        activity.supportFragmentManager.beginTransaction().remove(fragment).commitNow()
        assertEquals(listOf("created", "started", "resumed", "paused", "stopped"), viewModel.hooks)
        assertNull(fragment.previousState)
    }

    @Test
    fun `events are ignored by default`() {
        val activity = host(HostActivity::class.java)
        val fragment = SilentFragment()
        activity.add(fragment)
        fragment.viewModel.done()
        idleMain()
        assertTrue(fragment.viewModel.hooks.contains("resumed"))
    }

    @Test
    fun `the ViewModel belongs to the fragment by default`() {
        val activity = host(HostActivity::class.java)
        val first = TestFragment()
        val second = TestFragment()
        activity.add(first)
        activity.add(second)

        assertSame(first.lifecycleScope, first.scope())
        assertNotSame(first.viewModel, second.viewModel)
    }

    @Test
    fun `the ViewModel can belong to the activity`() {
        val activity = host(HostActivity::class.java)
        val first = TestFragment().apply { activityScope = true }
        val second = TestFragment().apply { activityScope = true }
        activity.add(first)
        activity.add(second)

        assertSame(activity.lifecycleScope, first.scope())
        assertSame(first.viewModel, second.viewModel)
    }

    @Test
    fun `the initializer of the arguments reaches the ViewModel`() {
        val strategy = BundleSerializerStrategy.serializable<NameInitializer>()
        val fragment = TestFragment().apply { setInitializer(NameInitializer("fragment"), strategy) }
        host(HostActivity::class.java).add(fragment)

        assertEquals(NameInitializer("fragment"), fragment.getInitializer(strategy))
        assertEquals(NameInitializer("fragment"), fragment.viewModel.initializer)
    }

    @Test
    fun `the back button finishes the activity when there are no more fragments`() {
        val activity = host(HostActivity::class.java)
        activity.add(TestFragment())

        activity.onBackPressedDispatcher.onBackPressed()

        assertTrue(activity.isFinishing)
    }

    @Test
    fun `navigating back with a result finishes the activity with the result`() {
        val activity = host(HostActivity::class.java)
        val fragment = TestFragment()
        activity.add(fragment)

        assertFalse(fragment.navigateBack("bye"))

        assertTrue(activity.isFinishing)
        assertEquals(EMA_RESULT_CODE, shadowOf(activity).resultCode)
        assertEquals("\"bye\"", shadowOf(activity).resultIntent.getStringExtra(EMA_RESULT_KEY))
    }

    @Test
    fun `navigating back pops the fragment and delivers the result to the previous one`() {
        val activity = host(HostActivity::class.java)
        val previous = TestFragment().apply { listenResults = true }
        val current = TestFragment()
        activity.add(previous)
        activity.add(current, backStack = true)

        assertTrue(current.navigateBack("hello"))
        idleMain()

        assertFalse(activity.isFinishing)
        assertEquals(listOf("hello"), previous.results)
        assertEquals(0, activity.supportFragmentManager.backStackEntryCount)
    }

    @Test
    fun `a result that is not of the expected type is ignored`() {
        val activity = host(HostActivity::class.java)
        val previous = TestFragment().apply { listenResults = true }
        activity.add(previous)

        activity.supportFragmentManager.setFragmentResult(
            EMA_RESULT_KEY,
            Bundle().apply {
                putString(EMA_RESULT_KEY, "{not json")
            }
        )
        activity.supportFragmentManager.setFragmentResult(EMA_RESULT_KEY, Bundle())
        idleMain()

        assertTrue(previous.results.isEmpty())
    }

    @Test
    fun `an activity that owns the back delegate handles the back button`() {
        val activity = host(DelegateOwnerActivity::class.java)
        activity.add(TestFragment())

        activity.onBackPressedDispatcher.onBackPressed()

        assertTrue(activity.delegated)
        assertFalse(activity.isFinishing)
    }

    @Test
    fun `an activity that does not own the back delegate lets the fragment navigate back`() {
        val activity = host(DelegateNotOwnerActivity::class.java)
        activity.add(TestFragment())

        activity.onBackPressedDispatcher.onBackPressed()

        assertTrue(activity.isFinishing)
    }

    @Test
    fun `a fragment that handles the back button manually does not register the listener`() {
        val activity = host(DelegateOwnerActivity::class.java)
        activity.add(TestFragment().apply { manualBack = true })

        activity.onBackPressedDispatcher.onBackPressed()

        assertFalse(activity.delegated)
    }

    private fun HostActivity.navHost(startDestination: String): NavHostFragment {
        val navHost = NavHostFragment()
        add(navHost)
        navHost.navController.graph = navHost.navController.createGraph(startDestination = startDestination) {
            fragment<StartFragment>("start")
            fragment<TestFragment>("ema")
        }
        idleMain()
        return navHost
    }

    @Test
    fun `inside a navigation graph the fragment navigates back through the NavController`() {
        val activity = host(HostActivity::class.java)
        val navHost = activity.navHost(startDestination = "start")
        navHost.navController.navigate("ema")
        idleMain()
        val fragment = navHost.childFragmentManager.primaryNavigationFragment as TestFragment

        assertTrue(fragment.navigateBack())
        idleMain()

        assertEquals("start", navHost.navController.currentDestination?.route)
        assertFalse(activity.isFinishing)
    }

    @Test
    fun `the start destination of a navigation graph finishes the activity`() {
        val activity = host(HostActivity::class.java)
        val navHost = activity.navHost(startDestination = "ema")
        val fragment = navHost.childFragmentManager.primaryNavigationFragment as TestFragment

        assertFalse(fragment.navigateBack())

        assertTrue(activity.isFinishing)
    }

    private fun HostActivity.answerPermissionRequest(granted: Boolean) {
        val request = shadowOf(this).lastRequestedPermission
        val grantResult = if (granted) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED
        // Delivered through the framework, which also ends the request in progress
        shadowOf(this).receiveResult(
            shadowOf(this).nextStartedActivityForResult.intent,
            Activity.RESULT_OK,
            Intent()
                .putExtra("android.content.pm.extra.REQUEST_PERMISSIONS_NAMES", request.requestedPermissions)
                .putExtra("android.content.pm.extra.REQUEST_PERMISSIONS_RESULTS", intArrayOf(grantResult))
        )
        idleMain()
    }

    private fun declarePermissions(): Application {
        val application = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(application.packageManager).getInternalMutablePackageInfo(application.packageName)
            .requestedPermissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        return application
    }

    @Test
    fun `the permission manager of a fragment requests permissions through the fragment`() {
        val application = declarePermissions()
        val activity = host(HostActivity::class.java)
        val fragment = TestFragment()
        activity.add(fragment)
        val scope = CoroutineScope(Dispatchers.Unconfined)

        val denied = scope.async { fragment.permissionManager.requestPermission(Manifest.permission.RECORD_AUDIO) }
        activity.answerPermissionRequest(granted = false)
        assertEquals(PermissionState.NOT_GRANTED, denied.getCompleted())

        val multiple = scope.async {
            fragment.permissionManager.requestMultiplePermission(Manifest.permission.RECORD_AUDIO)
        }
        activity.answerPermissionRequest(granted = true)
        assertEquals(mapOf(Manifest.permission.RECORD_AUDIO to PermissionState.GRANTED), multiple.getCompleted())

        shadowOf(application).grantPermissions(Manifest.permission.CAMERA)
        val granted = scope.async { fragment.permissionManager.requestPermission(Manifest.permission.CAMERA) }
        assertEquals(PermissionState.GRANTED, granted.getCompleted())
    }

    @Test
    fun `a request made while another one is in progress is cancelled instead of hanging`() {
        declarePermissions()
        val activity = host(HostActivity::class.java)
        val fragment = TestFragment()
        activity.add(fragment)
        val scope = CoroutineScope(Dispatchers.Unconfined)

        scope.async { fragment.permissionManager.requestPermission(Manifest.permission.RECORD_AUDIO) }
        // The system answers the second request synchronously with empty results
        val single = scope.async { fragment.permissionManager.requestPermission(Manifest.permission.CAMERA) }
        val multiple = scope.async {
            fragment.permissionManager.requestMultiplePermission(Manifest.permission.CAMERA)
        }

        assertEquals(PermissionState.NOT_GRANTED, single.getCompleted())
        assertEquals(emptyMap(), multiple.getCompleted())
    }
}
