package com.carmabs.emanate.android

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import android.text.SpannableString
import android.util.Size
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import com.carmabs.emanate.android.base.EmaSingleToast
import com.carmabs.emanate.android.base.showToast
import com.carmabs.emanate.android.configuration.Android
import com.carmabs.emanate.android.extension.addOnBackPressedListener
import com.carmabs.emanate.android.extension.copyDefault
import com.carmabs.emanate.android.extension.getRoundedCornerBitmap
import com.carmabs.emanate.android.extension.resizeCrop
import com.carmabs.emanate.android.extension.resizeCropSquare
import com.carmabs.emanate.android.extension.resizeFitInside
import com.carmabs.emanate.android.extension.toBitmap
import com.carmabs.emanate.android.extension.toBitmapWithMaxSize
import com.carmabs.emanate.android.extension.toByteArray
import com.carmabs.emanate.android.extra.EmaActivityResult
import com.carmabs.emanate.android.logging.EmaAndroidDataClassPrinter
import com.carmabs.emanate.android.permission.InfoDialogType
import com.carmabs.emanate.android.savestate.EmaSaveStateManager
import com.carmabs.emanate.android.savestate.SavedStateSupport
import com.carmabs.emanate.android.service.EmaJobService
import com.carmabs.emanate.android.viewmodel.EmaAndroidViewModel
import com.carmabs.emanate.android.viewmodel.EmaViewModelFactory
import com.carmabs.emanate.core.error.EmaErrorPolicy
import com.carmabs.emanate.core.initializer.EmaInitializer
import com.carmabs.emanate.core.model.EmaBackHandlerStrategy
import com.carmabs.emanate.core.model.EmaConfiguration
import com.carmabs.emanate.core.state.EmaEvent
import com.carmabs.emanate.core.state.EmaState
import com.carmabs.emanate.core.viewmodel.EmaViewModelClassic
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowToast

data class TestState(val value: Int = 0) : EmaState

class TestViewModel : EmaViewModelClassic<TestState, EmaEvent.EMPTY>(TestState()) {
    var destroyed = false
    override fun onStateCreated(initializer: EmaInitializer?) = Unit
    override fun onDestroy() {
        destroyed = true
    }

    fun currentScope() = scope

    fun launchWork(block: suspend () -> Unit) = sideEffect { block() }
}

@GraphicsMode(GraphicsMode.Mode.NATIVE)
@RunWith(RobolectricTestRunner::class)
class EmaBitmapExtensionsTest {

    private fun bitmap(width: Int, height: Int) =
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }

    @Test
    fun `rounded corners keep the size`() {
        val rounded = bitmap(20, 10).getRoundedCornerBitmap()
        assertEquals(20, rounded.width)
        assertEquals(10, rounded.height)
        assertEquals(Color.TRANSPARENT, rounded.getPixel(0, 0))
        assertEquals(Color.RED, rounded.getPixel(10, 5))
    }

    @Test
    fun `bitmaps are encoded and decoded`() = runTest {
        val bytes = bitmap(8, 8).toByteArray()
        assertTrue(bytes.isNotEmpty())
        assertEquals(8, bytes.toBitmap().width)
        assertEquals(4, bytes.toBitmap(width = 4, height = 4).width)
        assertEquals(8, bytes.toBitmap(colorTint = Color.BLUE).width)
    }

    @Test
    fun `copies are mutable by default`() {
        val original = bitmap(4, 4)
        val copy = original.copyDefault()
        assertNotSame(original, copy)
        assertTrue(copy.isMutable)
        assertFalse(original.copyDefault(mutable = false).isMutable)
    }

    @Test
    fun `crop to a square keeps the shorter side`() {
        assertEquals(10, bitmap(20, 10).resizeCropSquare().width)
        assertEquals(10, bitmap(10, 20).resizeCropSquare().height)
    }

    @Test
    fun `crop resizes to the width and crops the height`() {
        val cropped = bitmap(100, 100).resizeCrop(50, 20)
        assertEquals(50, cropped.width)
        assertEquals(20, cropped.height)
        val notCropped = bitmap(100, 10).resizeCrop(50, 20)
        assertEquals(5, notCropped.height)
    }

    @Test
    fun `fit inside keeps the destination size`() {
        assertEquals(Size(30, 30), bitmap(100, 50).resizeFitInside(30, 30).let { Size(it.width, it.height) })
        assertEquals(Size(30, 30), bitmap(50, 100).resizeFitInside(30, 30).let { Size(it.width, it.height) })
    }

    @Test
    fun `drawables are converted with a maximum size`() {
        val resources = ApplicationProvider.getApplicationContext<Application>().resources
        val wide = BitmapDrawable(resources, bitmap(40, 20))
        assertEquals(40, wide.toBitmapWithMaxSize().width)
        assertEquals(Size(10, 10), wide.toBitmapWithMaxSize(Size(10, 10)).let { Size(it.width, it.height) })
        val tall = BitmapDrawable(resources, bitmap(20, 40))
        assertEquals(Size(20, 20), tall.toBitmapWithMaxSize(Size(100, 20)).let { Size(it.width, it.height) })
        assertEquals(1, ColorDrawable(Color.RED).toBitmapWithMaxSize().width)
    }
}

@RunWith(RobolectricTestRunner::class)
class EmaBackHandlerTest {

    @Test
    fun `cancelled back presses are handled by the listener`() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        var presses = 0
        activity.addOnBackPressedListener {
            presses++
            EmaBackHandlerStrategy.Cancelled
        }
        shadowOf(Looper.getMainLooper()).idle()
        activity.onBackPressedDispatcher.onBackPressed()
        activity.onBackPressedDispatcher.onBackPressed()
        assertEquals(2, presses)
        assertFalse(activity.isFinishing)
    }

    @Test
    fun `continuing a back press lets the system handle it`() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        var presses = 0
        activity.addOnBackPressedListener(activity) {
            presses++
            EmaBackHandlerStrategy.ContinueOnBackPressed(removeBackHandler = true)
        }
        shadowOf(Looper.getMainLooper()).idle()
        activity.onBackPressedDispatcher.onBackPressed()
        assertEquals(1, presses)
        assertTrue(activity.isFinishing)
    }

    @Test
    fun `the handler can be kept after continuing`() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        var presses = 0
        val handler = activity.addOnBackPressedListener(activity) {
            presses++
            EmaBackHandlerStrategy.ContinueOnBackPressed(removeBackHandler = false)
        }
        shadowOf(Looper.getMainLooper()).idle()
        activity.onBackPressedDispatcher.onBackPressed()
        handler.remove()
        activity.onBackPressedDispatcher.onBackPressed()
        handler.restore()
        handler.restore()
        activity.onBackPressedDispatcher.onBackPressed()
        assertEquals(2, presses)
    }

    @Test
    fun `the handler is removed and added again with the lifecycle`() {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val activity = controller.get()
        var presses = 0
        activity.addOnBackPressedListener(activity) {
            presses++
            EmaBackHandlerStrategy.Cancelled
        }
        shadowOf(Looper.getMainLooper()).idle()
        controller.pause().stop()
        controller.start().resume()
        shadowOf(Looper.getMainLooper()).idle()
        activity.onBackPressedDispatcher.onBackPressed()
        assertEquals(1, presses)
        assertTrue(activity.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
    }
}

@RunWith(RobolectricTestRunner::class)
class EmaAndroidViewModelTest {

    @Test
    fun `the android ViewModel gives its scope to the Ema ViewModel and clears it`() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val emaViewModel = TestViewModel()
        val androidViewModel = ViewModelProvider(activity, EmaViewModelFactory({ emaViewModel }))[
            emaViewModel.id, EmaAndroidViewModel::class.java
        ]
        assertSame(emaViewModel, androidViewModel.emaViewModel)
        assertTrue(emaViewModel.currentScope().isActive)

        activity.viewModelStore.clear()
        assertTrue(emaViewModel.destroyed)
        assertFalse(emaViewModel.currentScope().isActive)
    }

    @Test
    fun `the view model is only created if the screen doesn't have it yet`() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        var created = 0
        val factory = EmaViewModelFactory({ TestViewModel().also { created++ } })

        val first = ViewModelProvider(activity, factory)["screen", EmaAndroidViewModel::class.java]
        val second = ViewModelProvider(activity, factory)["screen", EmaAndroidViewModel::class.java]

        assertSame(first, second)
        assertEquals(1, created)
    }

    @Test
    fun `the provided saved state handle is used`() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val handle = SavedStateHandle(mapOf("key" to "value"))
        val androidViewModel = ViewModelProvider(activity, EmaViewModelFactory({ TestViewModel() }, handle))[
            "with handle", EmaAndroidViewModel::class.java
        ]
        assertSame(handle, androidViewModel.savedStateHandle)
    }

    @Test
    fun `save state support keeps the handle and the manager`() {
        val handle = SavedStateHandle()
        val manager =
            EmaSaveStateManager<TestState, EmaEvent.EMPTY> { _, saveStateHandle, _ ->
                saveStateHandle["saved"] =
                    true
            }
        val support = SavedStateSupport(handle, manager)
        support.saveStateManager.onSaveStateHandling(
            CoroutineScope(Dispatchers.Unconfined),
            support.savedStateHandle,
            TestViewModel()
        )
        assertEquals(true, handle.get<Boolean>("saved"))
    }
}

@RunWith(RobolectricTestRunner::class)
class EmaAndroidConfigurationTest {

    @Test
    fun `android configuration uses the android implementations`() {
        val configuration = EmaConfiguration.Android(debug = true)
        assertSame(Dispatchers.Main.immediate, configuration.dispatchers.main)
        assertSame(Dispatchers.IO, configuration.dispatchers.io)
        assertIs<EmaAndroidDataClassPrinter>(configuration.printer)
    }

    @Test
    fun `android configuration logs and throws only in debug`() {
        val debug = EmaConfiguration.Android(debug = true)
        assertNotNull(debug.logger)
        assertSame(EmaErrorPolicy.Throw, debug.errorPolicy)

        val release = EmaConfiguration.Android(debug = false)
        assertNull(release.logger)
        assertIs<EmaErrorPolicy.Catch>(release.errorPolicy)
    }
}

class EmaAndroidDataClassPrinterTest {

    private enum class Role { ADMIN }
    private data class Address(val street: String)
    private data class User(
        val name: String,
        val role: Role,
        val tags: List<String>,
        val extra: Map<String, Int>,
        val address: Address?,
        val id: UUID
    )
    private class Node(val name: String) {
        var next: Node? = null
    }

    @Test
    fun `objects are printed with their fields indented`() {
        val output = EmaAndroidDataClassPrinter().print(
            User("Ana", Role.ADMIN, listOf("a", "b"), mapOf("x" to 1), Address("Main"), UUID(0, 1))
        )
        assertTrue(output.startsWith("User("))
        assertTrue(output.contains("name = \"Ana\""))
        assertTrue(output.contains("Role.ADMIN"))
        assertTrue(output.contains("\"a\""))
        assertTrue(output.contains("\"x\" -> 1"))
        assertTrue(output.contains("Address("))
        assertTrue(output.contains("00000000-0000-0000-0000-000000000001"))
    }

    @Test
    fun `atomic values, nulls and long strings`() {
        val printer = EmaAndroidDataClassPrinter(indent = 4, wrappedLineWidth = 10)
        assertEquals("1\n", printer.print(1))
        assertEquals("null\n", printer.print(null))
        assertTrue(printer.print("a long text that needs several lines").lines().size > 2)
        assertEquals("[\n]\n", printer.print(emptyList<Int>()))
        assertTrue(printer.print(listOf(1, 2)).contains("1,"))
    }

    @Test
    fun `cyclic references are detected`() {
        val first = Node("first")
        val second = Node("second").also { it.next = first }
        first.next = second
        assertTrue(EmaAndroidDataClassPrinter().print(first).contains("cyclic reference detected"))
    }
}

@RunWith(RobolectricTestRunner::class)
class EmaAndroidSmallComponentsTest {

    private val application = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun `single toast shows each message once while it is visible`() {
        EmaSingleToast.show(application, "Hello", Toast.LENGTH_SHORT)
        assertEquals("Hello", ShadowToast.getTextOfLatestToast().toString())
        application.showToast("", Toast.LENGTH_SHORT)
        assertEquals("", ShadowToast.getTextOfLatestToast().toString())
    }

    @Test
    fun `data holders`() {
        val result = EmaActivityResult(1, 2, Intent("action"))
        assertEquals(1, result.requestCode)
        assertEquals(2, result.resultCode)
        assertEquals("action", result.data?.action)
        val dialog = InfoDialogType.Default("title", "message")
        assertEquals("title", dialog.title)
        assertEquals("message", dialog.message)
    }

    class TestJobService : EmaJobService() {
        override suspend fun executeWork(intent: Intent, resultReceiver: ResultReceiver?) {
            resultReceiver?.send(1, Bundle().apply { putString("action", intent.action) })
        }

        fun handle(intent: Intent) = onHandleWork(intent)
    }

    @Test
    fun `job service executes the work and notifies the listener`() {
        val latch = CountDownLatch(1)
        var action: String? = null
        val listener = object : EmaJobService.EmaJobServiceListener(Handler(Looper.getMainLooper())) {
            override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                action = resultData?.getString("action")
                latch.countDown()
            }
        }
        val intent = Intent("work").putExtra("EMA_SERVICE_LISTENER", listener)
        Robolectric.buildService(TestJobService::class.java).create().get().handle(intent)

        for (i in 0 until 50) {
            shadowOf(Looper.getMainLooper()).idle()
            if (latch.await(20, TimeUnit.MILLISECONDS)) break
        }
        assertEquals("work", action)
    }

    @Test
    fun `job service enqueues its work`() {
        EmaJobService.enqueueWork(application, application.packageName, TestJobService::class.java.name, 7)
        val jobScheduler = application.getSystemService(android.app.job.JobScheduler::class.java)
        val job = jobScheduler.allPendingJobs.single { it.id == 7 }
        assertEquals(ComponentName(application.packageName, TestJobService::class.java.name), job.service)
    }
}
