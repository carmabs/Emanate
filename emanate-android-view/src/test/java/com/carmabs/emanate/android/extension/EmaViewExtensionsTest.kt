@file:Suppress("DEPRECATION")

package com.carmabs.emanate.android.extension

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.TransitionDrawable
import android.os.Looper
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.createGraph
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.fragment
import com.carmabs.emanate.android.HostActivity
import com.carmabs.emanate.android.NameInitializer
import com.carmabs.emanate.android.ViewEvent
import com.carmabs.emanate.android.ViewState
import com.carmabs.emanate.android.ViewTestViewModel
import com.carmabs.emanate.android.delegates.emaStateDelegate
import com.carmabs.emanate.android.idleMain
import com.carmabs.emanate.android.initializer.EmaInitializerBundle
import com.carmabs.emanate.android.initializer.bundle.strategy.BundleSerializerStrategy
import com.carmabs.emanate.android.utils.Android
import com.carmabs.emanate.core.initializer.EmaInitializerSerializer
import com.carmabs.emanate.core.navigator.EmaNavigator
import com.carmabs.emanate.core.utils.EmaUtils
import com.carmabs.emanate.core.view.EmaView
import com.carmabs.emanate.core.view.EmaViewModelTrigger
import java.time.Duration
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

class BindView : EmaView<ViewState, ViewTestViewModel, ViewEvent> {
    override val coroutineScope: CoroutineScope = MainScope()
    override val viewModel = ViewTestViewModel()
    override val navigator: EmaNavigator<ViewEvent>? = null
    override val startTrigger: EmaViewModelTrigger? = null
    override val initializerSerializer: EmaInitializerSerializer? = null
    override var previousState: ViewState? = null
    override fun onState(state: ViewState) = Unit
    override suspend fun onEvent(event: ViewEvent) = Unit
    override fun onBack(result: Any?) = false
}

data class NotAState(val other: Int)

class ContentFragment : Fragment()

@RunWith(RobolectricTestRunner::class)
class EmaBindForUpdateTest {

    @Test
    fun `without previous state the action always runs`() {
        val view = BindView()
        val state = ViewState(count = 1)
        val values = mutableListOf<Int>()
        val pairs = mutableListOf<Pair<Int?, Int>>()

        view.bindForUpdate(state::count) { values += it }
        view.bindForUpdateWithPrevious(state::count) { old, new -> pairs += old to new }

        assertEquals(listOf(1), values)
        assertEquals(listOf<Pair<Int?, Int>>(null to 1), pairs)
    }

    @Test
    fun `the action only runs when the field changes`() {
        val view = BindView().apply { previousState = ViewState(count = 1, name = "a") }
        val values = mutableListOf<Int>()
        val pairs = mutableListOf<Pair<Int?, Int>>()

        assertFalse(view.bindForUpdate(ViewState(count = 1)::count) { values += it })
        assertTrue(view.bindForUpdate(ViewState(count = 2)::count) { values += it })
        assertFalse(view.bindForUpdateWithPrevious(ViewState(count = 1)::count) { old, new -> pairs += old to new })
        assertTrue(view.bindForUpdateWithPrevious(ViewState(count = 3)::count) { old, new -> pairs += old to new })

        assertEquals(listOf(2), values)
        assertEquals(listOf<Pair<Int?, Int>>(1 to 3), pairs)
    }

    @Test
    fun `a comparator decides when the field has changed`() {
        val view = BindView().apply { previousState = ViewState(name = "a") }
        val ignoreCase = { old: String, new: String -> old.equals(new, ignoreCase = true) }

        assertFalse(view.bindForUpdate(ViewState(name = "A")::name, ignoreCase) { })
        assertTrue(view.bindForUpdate(ViewState(name = "b")::name, ignoreCase) { })
        assertFalse(view.bindForUpdateWithPrevious(ViewState(name = "A")::name, ignoreCase) { _, _ -> })
        assertTrue(view.bindForUpdateWithPrevious(ViewState(name = "b")::name, ignoreCase) { _, _ -> })
    }

    @Test
    fun `a field that is not in the state is not bound`() {
        val view = BindView().apply { previousState = ViewState() }
        val other = NotAState(5)
        var called = false

        assertFalse(view.bindForUpdate(other::other) { called = true })
        assertFalse(view.bindForUpdateWithPrevious(other::other) { _, _ -> called = true })
        assertFalse(called)
    }
}

@RunWith(RobolectricTestRunner::class)
class EmaViewExtensionsTest {

    private val activity = Robolectric.buildActivity(HostActivity::class.java).setup().get()
    private val looper = shadowOf(Looper.getMainLooper())
    private lateinit var defaultLocale: Locale

    @Before
    fun setLocale() {
        defaultLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @After
    fun restoreLocale() {
        Locale.setDefault(defaultLocale)
    }

    @Test
    fun `afterMeasured waits until the view has a size`() {
        val view = View(activity)
        var measured = 0
        view.afterMeasured { measured++ }
        assertEquals(0, measured)

        activity.setContentView(view)
        idleMain()
        assertEquals(1, measured)

        view.afterMeasured { measured++ }
        assertEquals(2, measured)
    }

    @Test
    fun `the integer increment animates from the current number`() {
        val textView = TextView(activity).apply { text = "5 pts" }

        textView.setIncrementAnimated(10, "%d pts", duration = 100)
        assertEquals("5 pts", textView.text.toString())
        looper.idleFor(Duration.ofSeconds(1))
        assertEquals("10 pts", textView.text.toString())

        TextView(activity).apply {
            text = "none"
            setIncrementAnimated(3, null, duration = 100)
            looper.idleFor(Duration.ofSeconds(1))
            assertEquals("3", text.toString())
        }
    }

    @Test
    fun `the decimal increment animates from the current number`() {
        val textView = TextView(activity).apply { text = "3.0" }

        textView.setIncrementAnimated(5f, null, duration = 100)
        assertEquals("3.0", textView.text.toString())
        looper.idleFor(Duration.ofSeconds(1))
        assertEquals("5.0", textView.text.toString())

        TextView(activity).apply {
            text = "2.5 €"
            setIncrementAnimated(1f, "%.1f €", duration = 100)
            assertEquals("2.5 €", text.toString())
            looper.idleFor(Duration.ofSeconds(1))
            assertEquals("1.0 €", text.toString())
        }

        TextView(activity).apply {
            text = "none"
            setIncrementAnimated(1f, null, duration = 100)
            assertEquals("0.0", text.toString())
        }
    }

    @Test
    fun `the cursor is placed at the end of the text`() {
        val editText = EditText(activity)
        editText.setTextWithCursorAtEnd("hello")
        assertEquals("hello", editText.text.toString())
        assertEquals(5, editText.selectionStart)
    }

    @Test
    fun `the keyboard is hidden from activities, fragments and views`() {
        val fragment = ContentFragmentWithView()
        activity.supportFragmentManager.beginTransaction().add(activity.containerId, fragment).commitNow()
        val editText = fragment.requireView() as EditText
        val inputMethodManager = activity.getSystemService(InputMethodManager::class.java)
        val keyboardVisible = { shadowOf(inputMethodManager).isSoftInputVisible }

        inputMethodManager.showSoftInput(editText, 0)
        assertTrue(keyboardVisible())
        fragment.hideKeyboard()
        assertFalse(keyboardVisible())

        inputMethodManager.showSoftInput(editText, 0)
        activity.hideKeyboard()
        assertFalse(keyboardVisible())

        inputMethodManager.showSoftInput(editText, 0)
        editText.hideKeyboard()
        assertFalse(keyboardVisible())
    }

    @Test
    fun `an image is replaced with a cross fade only when it already has one`() {
        val imageView = ImageView(activity)
        val drawable = ColorDrawable(Color.RED)

        imageView.setImageDrawableWithTransition(drawable)
        assertNull(imageView.drawable)

        imageView.setImageDrawable(ColorDrawable(Color.BLUE))
        imageView.setImageDrawableWithTransition(drawable)
        assertIs<TransitionDrawable>(imageView.drawable)
    }

    @Test
    fun `an image is set with an overshoot rotation`() {
        val imageView = ImageView(activity)
        var ended = 0

        imageView.setImageWithOvershot(ColorDrawable(Color.RED), endAnimationListener = { ended++ })
        assertEquals(-10f, imageView.rotation)
        looper.idleFor(Duration.ofSeconds(1))
        assertEquals(1, ended)
        assertEquals(0f, imageView.rotation)

        imageView.setImageWithOvershot(ColorDrawable(Color.BLUE), rightDirection = false)
        assertEquals(10f, imageView.rotation)
        looper.idleFor(Duration.ofSeconds(1))
    }

    @Test
    fun `visibility and update helpers`() {
        assertEquals(View.VISIBLE, EmaUtils.View.Android.visibility(true))
        assertEquals(View.GONE, EmaUtils.View.Android.visibility(false))
        assertEquals(View.INVISIBLE, EmaUtils.View.Android.visibility(false, gone = false))

        val updates = mutableListOf<Int>()
        EmaUtils.Values.checkUpdate(1, 1) { updates += it }
        EmaUtils.Values.checkUpdate(1, 2) { updates += it }
        assertEquals(listOf(2), updates)
    }

    @Test
    fun `the progress bar is tinted`() {
        val progressBar = ProgressBar(activity)
        progressBar.setProgressTintCompat(Color.RED)
        assertEquals(Color.RED, progressBar.progressTintList?.defaultColor)
        progressBar.setProgressGradientTintCompat(Color.BLUE)
        assertEquals(Color.BLUE, progressBar.progressTintList?.defaultColor)
    }

    @Test
    fun `the color of a gradient is interpolated between its stops`() {
        val colors = intArrayOf(Color.BLACK, Color.WHITE, Color.RED)
        val positions = floatArrayOf(0f, 0.5f, 1f)

        assertEquals(Color.BLACK, EmaUtils.Color.Android.fromGradient(colors, positions, -1f))
        assertEquals(Color.RED, EmaUtils.Color.Android.fromGradient(colors, positions, 2f))
        assertEquals(Color.argb(255, 127, 127, 127), EmaUtils.Color.Android.fromGradient(colors, positions, 0.25f))
        assertEquals(Color.WHITE, EmaUtils.Color.Android.fromGradient(colors, positions, 0.5f))
        assertEquals(
            Color.GREEN,
            EmaUtils.Color.Android.fromGradient(intArrayOf(Color.GREEN), floatArrayOf(0.3f), 0.9f)
        )
        assertFailsWith<IllegalArgumentException> { EmaUtils.Color.Android.fromGradient(colors, floatArrayOf(0f), 0f) }
        assertFailsWith<IllegalArgumentException> {
            EmaUtils.Color.Android.fromGradient(intArrayOf(), floatArrayOf(), 0f)
        }
        assertEquals(Color.argb(255, 127, 127, 127), EmaUtils.Color.Android.lerp(Color.BLACK, Color.WHITE, 0.5f))
    }

    @Test
    fun `animator sets are controlled with their children`() {
        val first = ValueAnimator.ofFloat(0f, 1f).setDuration(10_000)
        val second = ValueAnimator.ofFloat(0f, 1f).setDuration(10_000)
        val set = AnimatorSet().apply { playTogether(first, second) }
        assertFalse(set.isSomeRunning())

        set.start()
        assertTrue(set.isSomeRunning())

        set.pauseAll()
        assertTrue(set.isPaused)
        set.resumeAll()
        assertFalse(set.isPaused)

        set.endAll()
        assertFalse(set.isSomeRunning())

        set.start()
        set.cancelAll()
        assertFalse(set.isSomeRunning())
        assertFalse(first.isSomeRunning())
    }

    @Test
    fun `the state delegate creates the initial value lazily`() {
        var creations = 0
        var value: String by emaStateDelegate {
            creations++
            "initial"
        }
        assertEquals(0, creations)
        assertEquals("initial", value)
        assertEquals("initial", value)
        assertEquals(1, creations)
        value = "changed"
        assertEquals("changed", value)
    }

    @Test
    fun `the NavController navigates with an initializer`() {
        val navHost = NavHostFragment()
        activity.supportFragmentManager.beginTransaction().add(activity.containerId, navHost).commitNow()
        val navController = navHost.navController
        navController.graph = navController.createGraph(startDestination = "start") {
            fragment<ContentFragment>("start")
            fragment<ContentFragment>("detail")
        }
        val strategy = BundleSerializerStrategy.serializable<NameInitializer>()
        val detailId = navController.graph.findNode("detail")!!.id

        navController.navigate(detailId, EmaInitializerBundle(NameInitializer("nav"), strategy))
        idleMain()

        assertEquals("detail", navController.currentDestination?.route)
        assertEquals(
            NameInitializer("nav"),
            navController.currentBackStackEntry?.arguments?.getInitializer<NameInitializer>(strategy)
        )
    }
}

class ContentFragmentWithView : Fragment() {
    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: android.os.Bundle?
    ): View = EditText(inflater.context)
}
