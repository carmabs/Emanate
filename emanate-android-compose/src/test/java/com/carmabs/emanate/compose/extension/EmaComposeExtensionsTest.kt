package com.carmabs.emanate.compose.extension

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.view.ContextThemeWrapper
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.carmabs.emanate.compose.ClassicViewModel
import com.carmabs.emanate.compose.CounterAction
import com.carmabs.emanate.compose.CounterEvent
import com.carmabs.emanate.compose.CounterState
import com.carmabs.emanate.compose.CounterViewModel
import com.carmabs.emanate.compose.list.toImmutable
import com.carmabs.emanate.compose.permission.rememberEmaPermissionManager
import com.carmabs.emanate.compose.test.R
import com.carmabs.emanate.compose.utils.Compose
import com.carmabs.emanate.core.manager.EmaPermissionManager
import com.carmabs.emanate.core.manager.PermissionState
import com.carmabs.emanate.core.model.EmaImage
import com.carmabs.emanate.core.model.EmaMultiplePermissionRequest
import com.carmabs.emanate.core.model.EmaPermissionRequest
import com.carmabs.emanate.core.model.EmaText
import com.carmabs.emanate.core.utils.EmaUtils
import com.carmabs.emanate.core.value.EmaUriRes
import com.carmabs.emanate.core.value.EmaUriType
import com.carmabs.emanate.core.viewmodel.EmaViewModelAction
import java.io.ByteArrayOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.GraphicsMode

@OptIn(ExperimentalCoroutinesApi::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@RunWith(RobolectricTestRunner::class)
class EmaComposeExtensionsTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val application = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun `texts are resolved from resources and formats`() {
        val texts = mutableListOf<String>()
        composeRule.setContent {
            texts += EmaText.id(android.R.string.ok).stringResource()
            texts += EmaText.id(R.string.test_greeting, "Ema").stringResource()
            texts += EmaText.plural(R.plurals.test_items, 1).stringResource()
            texts += EmaText.plural(R.plurals.test_items, 3, 3).stringResource()
            texts += EmaText.text("plain 50%").stringResource()
            texts += EmaText.text("Hello %s", "text").stringResource()
            texts += EmaText.composition(EmaText.text("A"), EmaText.text("B")).stringResource()
            texts += R.string.test_greeting.toComposeString("res")
            texts += android.R.string.ok.toComposeString()
            texts += android.R.string.ok.toComposeString(*emptyArray())
        }
        composeRule.waitForIdle()

        assertEquals(
            listOf("OK", "Hello Ema", "%d item", "3 items", "plain 50%", "Hello text", "AB", "Hello res", "OK", "OK"),
            texts.take(10)
        )
    }

    @Test
    fun `resources and images are converted to compose types`() {
        val bytes = ByteArrayOutputStream().also {
            Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.PNG, 100, it)
        }.toByteArray()
        val painters = mutableListOf<Painter>()
        var color: Color? = null
        var vector: ImageVector? = null
        composeRule.setContent {
            color = R.color.test_red.toComposeColor()
            vector = R.drawable.test_vector.toComposeImageVector()
            painters += R.drawable.test_vector.toComposePainter()
            painters += EmaImage.Id(R.drawable.test_vector).toComposePainter()
            painters += EmaImage.Uri(EmaUriRes("drawable/test_vector", EmaUriType.Drawable)).toComposePainter()
            painters += EmaImage.ByteArray(bytes).toComposePainter()
            EmaImage.Id(R.drawable.test_vector).toComposableImage(contentDescription = "vector")
        }
        composeRule.waitForIdle()

        assertEquals(Color.Red, color)
        assertNotNull(vector)
        assertEquals(4, painters.size)
    }

    @Test
    fun `the screen dimensions are read from the configuration`() {
        val values = mutableListOf<Float>()
        var dps = emptyList<Dp>()
        var sp: TextUnit? = null
        composeRule.setContent {
            values += EmaUtils.Screen.Compose.heightPx()
            values += EmaUtils.Screen.Compose.widthPx()
            values += 10.dp.toPx()
            dps =
                listOf(EmaUtils.Screen.Compose.heightDp(), EmaUtils.Screen.Compose.widthDp(), 10.pxToDp(), 10f.pxToDp())
            sp = 10.dp.inSp
        }
        composeRule.waitForIdle()

        val density = composeRule.activity.resources.displayMetrics.density
        assertTrue(values.take(2).all { it > 0 })
        assertEquals(10 * density, values[2])
        assertTrue(dps[0] > 0.dp && dps[1] > 0.dp)
        assertEquals((10 / density).dp, dps[3])
        assertEquals(dps[2].value, dps[3].value, 0.01f)
        assertNotNull(sp)
    }

    @Test
    fun `a view is measured before the content is drawn`() {
        var measured: Dp? = null
        composeRule.setContent {
            EmaMeasureViewWidth(
                viewToMeasure = { Box(Modifier.width(50.dp).height(10.dp)) },
                content = { width, view ->
                    measured = width
                    view()
                }
            )
        }
        composeRule.waitForIdle()

        assertEquals(50f, measured!!.value, 1f)
    }

    @Test
    fun `the fade modifier draws on every side`() {
        composeRule.setContent {
            Column {
                FadeSide.entries.forEach { side ->
                    Box(Modifier.size(10.dp).fade(side).background(Color.Red))
                }
                Box(Modifier.size(10.dp).fade().background(Color.Blue))
            }
        }

        assertTrue(composeRule.onRoot().captureToImage().width > 0)
    }

    @Test
    fun `the animated visibility listeners follow the transition`() {
        val visible = mutableStateOf(false)
        val calls = mutableListOf<String>()
        composeRule.setContent {
            AnimatedVisibility(visible.value) {
                setOnBeforeVisibleListener { calls += "before" }
                setOnVisibleListener { calls += "visible" }
                setOnHideListener { calls += "hide" }
                BasicText("Content")
            }
        }

        visible.value = true
        composeRule.waitForIdle()
        visible.value = false
        composeRule.waitForIdle()

        assertTrue(calls.containsAll(listOf("before", "visible", "hide")))
    }

    @Test
    fun `the preview helpers depend on the inspection mode`() {
        val results = mutableListOf<Any?>()
        composeRule.setContent {
            results += EmaUtils.Preview.Compose.isInPreview()
            results += "real".changeForPreview("preview")
            EmaUtils.Preview.Compose.skipForPreview(previewComposable = { results += "preview content" }) {
                results +=
                    "content"
            }
            CompositionLocalProvider(LocalInspectionMode provides true) {
                results += EmaUtils.Preview.Compose.isInPreview()
                results += "real".changeForPreview("preview")
                EmaUtils.Preview.Compose.skipForPreview(previewComposable = { results += "preview content" }) {
                    results +=
                        "content"
                }
                EmaUtils.Preview.Compose.skipForPreview { results += "not in preview" }
            }
        }
        composeRule.waitForIdle()

        assertEquals(listOf(false, "real", "content", true, "preview", "preview content"), results.take(6))
    }

    @Test
    fun `the activity is found through the wrappers of the context`() {
        val activities = mutableListOf<ComponentActivity>()
        composeRule.setContent {
            activities += LocalContext.activity
            CompositionLocalProvider(LocalContext provides ContextThemeWrapper(LocalContext.current, 0)) {
                activities += LocalContext.activity
            }
        }
        composeRule.waitForIdle()

        assertTrue(activities.all { it === composeRule.activity })
    }

    @Test
    fun `ViewModels are cast to their action types`() {
        val viewModel = CounterViewModel()
        assertSame<Any>(viewModel, viewModel.asViewModelAction<CounterState, CounterAction, CounterEvent>())
        assertSame<Any>(viewModel, viewModel.asActionDispatcher<CounterAction>())
        assertIs<EmaViewModelAction<*, *, *>>(viewModel.asViewModelAction<CounterState, CounterAction, CounterEvent>())

        val classic = ClassicViewModel()
        assertFailsWith<IllegalStateException> {
            classic.asViewModelAction<CounterState, CounterAction, CounterEvent>()
        }
        assertFailsWith<IllegalStateException> { classic.asActionDispatcher<CounterAction>() }
    }

    @Test
    fun `small helpers`() {
        val list = listOf(1, 2).toImmutable()
        assertEquals(2, list.size)
        assertEquals(2, list[1])
        assertEquals(5, LoremIpsum(5).generate().split(" ").size)
    }

    private fun declarePermissions() {
        shadowOf(application.packageManager).getInternalMutablePackageInfo(application.packageName)
            .requestedPermissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    }

    private fun Activity.answerPermissionRequest(granted: Boolean) {
        val request = shadowOf(this).lastRequestedPermission
        val grantResult = if (granted) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED
        composeRule.runOnUiThread {
            shadowOf(this).receiveResult(
                shadowOf(this).nextStartedActivityForResult.intent,
                Activity.RESULT_OK,
                Intent()
                    .putExtra("android.content.pm.extra.REQUEST_PERMISSIONS_NAMES", request.requestedPermissions)
                    .putExtra("android.content.pm.extra.REQUEST_PERMISSIONS_RESULTS", intArrayOf(grantResult))
            )
        }
        composeRule.waitForIdle()
    }

    @Test
    fun `the permission manager requests the permissions through the activity`() {
        declarePermissions()
        lateinit var manager: EmaPermissionManager
        composeRule.setContent { manager = rememberEmaPermissionManager() }
        composeRule.waitForIdle()
        val scope = CoroutineScope(Dispatchers.Unconfined)

        val denied = scope.async { manager.requestPermission(Manifest.permission.RECORD_AUDIO) }
        composeRule.activity.answerPermissionRequest(granted = false)
        assertEquals(PermissionState.NOT_GRANTED, denied.getCompleted())

        val multiple = scope.async { manager.requestMultiplePermission(Manifest.permission.RECORD_AUDIO) }
        composeRule.activity.answerPermissionRequest(granted = true)
        assertEquals(mapOf(Manifest.permission.RECORD_AUDIO to PermissionState.GRANTED), multiple.getCompleted())

        shadowOf(application).grantPermissions(Manifest.permission.CAMERA)
        val granted = scope.async { manager.requestPermission(Manifest.permission.CAMERA) }
        assertEquals(PermissionState.GRANTED, granted.getCompleted())
    }

    @Test
    fun `in a preview every permission is granted`() = runTest {
        lateinit var manager: EmaPermissionManager
        composeRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) { manager = rememberEmaPermissionManager() }
        }
        composeRule.waitForIdle()
        val camera = Manifest.permission.CAMERA
        val audio = Manifest.permission.RECORD_AUDIO
        val single = mutableListOf<PermissionState>()
        val multiple = mutableListOf<Map<String, PermissionState>>()

        assertEquals(PermissionState.GRANTED, manager.requestPermission(camera))
        assertEquals(
            mapOf(camera to PermissionState.GRANTED, audio to PermissionState.GRANTED),
            manager.requestMultiplePermission(camera, audio)
        )
        assertEquals(PermissionState.GRANTED, manager.isPermissionGranted(camera))
        assertTrue(manager.areAllPermissionsGranted(camera, audio))
        assertTrue(manager.shouldShowRequestPermissionRationale(camera))
        assertEquals(PermissionState.GRANTED, manager.requestCoarseLocationPermission())
        assertEquals(PermissionState.GRANTED, manager.requestFineLocationPermission())
        assertEquals(PermissionState.GRANTED, manager.isLocationFineGranted())
        assertEquals(PermissionState.GRANTED, manager.isLocationBackgroundGranted())
        assertEquals(PermissionState.GRANTED, manager.isLocationCoarseGranted())

        val request = EmaPermissionRequest.createRequest { single += it }
        manager.handleRequest(request, this, camera)
        manager.responseRequest(request, PermissionState.NOT_GRANTED)
        val multipleRequest = EmaMultiplePermissionRequest.createRequest { multiple += it }
        manager.handleRequestMultiple(multipleRequest, this, camera, audio)
        manager.responseRequest(multipleRequest, mapOf(camera to PermissionState.NOT_GRANTED))

        assertEquals(listOf(PermissionState.GRANTED, PermissionState.NOT_GRANTED), single)
        assertEquals(
            listOf(
                mapOf(camera to PermissionState.GRANTED, audio to PermissionState.GRANTED),
                mapOf(camera to PermissionState.NOT_GRANTED)
            ),
            multiple
        )
    }
}
