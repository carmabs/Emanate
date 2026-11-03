@file:Suppress("DEPRECATION")

package com.carmabs.emanate.android.ui

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import com.carmabs.emanate.android.HostActivity
import com.carmabs.emanate.android.TestBinding
import com.carmabs.emanate.android.extra.EmaEditText
import com.carmabs.emanate.android.extra.EmaTextWatcher
import com.carmabs.emanate.android.idleMain
import com.carmabs.emanate.android.ui.dialog.EmaAndroidDialogProvider
import com.carmabs.emanate.android.ui.dialog.EmaDialog
import com.carmabs.emanate.core.dialog.EmaDialogData
import com.carmabs.emanate.core.dialog.EmaDialogListener
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

data class TestDialogData(
    val title: String = "",
    override val proportionWidth: Float? = null,
    override val proportionHeight: Float? = null,
    override val isModal: Boolean = false
) : EmaDialogData

class TestDialog : EmaDialog<TestBinding, TestDialogData>() {
    val setups = mutableListOf<TestDialogData>()
    var backDisabled: Boolean? = null

    override val disableBackButton get() = backDisabled ?: super.disableBackButton

    override fun createViewBinding(inflater: LayoutInflater, container: ViewGroup?) = TestBinding(inflater.context)

    override fun TestBinding.setup(data: TestDialogData) {
        setups += data
        text.text = data.title
    }

    override fun createInitialState() = TestDialogData(title = "initial")
}

class TestDialogProvider(fragmentManager: androidx.fragment.app.FragmentManager, tag: String? = null) :
    EmaAndroidDialogProvider(fragmentManager, tag) {
    var generated = 0
    override fun generateDialog(dialogData: EmaDialogData?) = TestDialog().also { generated++ }
}

class RecordingDialogListener : EmaDialogListener {
    val calls = mutableListOf<String>()
    override fun onBackPressed() {
        calls += "back"
    }

    override fun onDestroyed() {
        calls += "destroyed"
    }
}

@RunWith(RobolectricTestRunner::class)
class EmaDialogTest {

    private val activity = Robolectric.buildActivity(HostActivity::class.java).setup().get()
    private val fragmentManager = activity.supportFragmentManager

    private fun TestDialogProvider.shown(tag: String = TestDialogProvider::class.java.name): TestDialog {
        idleMain()
        return fragmentManager.findFragmentByTag(tag) as TestDialog
    }

    @Test
    fun `the provider shows, updates and hides a single dialog`() {
        val provider = TestDialogProvider(fragmentManager)
        val listener = RecordingDialogListener()
        provider.dialogListener = listener

        provider.show(TestDialogData("Hello", proportionWidth = 0.5f, proportionHeight = 0.5f))
        val dialog = provider.shown()

        assertTrue(provider.isVisible)
        assertEquals("Hello", dialog.data.title)
        assertEquals("Hello", dialog.setups.last().title)
        assertSame(listener, dialog.dialogListener)
        assertTrue(dialog.dialog!!.window!!.attributes.width > 0)

        provider.show(TestDialogData("World"))
        idleMain()
        assertEquals(1, provider.generated)
        assertEquals("World", dialog.data.title)

        val newListener = RecordingDialogListener()
        provider.dialogListener = newListener
        assertSame(newListener, dialog.dialogListener)

        provider.hide()
        idleMain()
        assertFalse(provider.isVisible)
        assertNull(fragmentManager.findFragmentByTag(TestDialogProvider::class.java.name))
        assertEquals(listOf("destroyed"), newListener.calls)
    }

    @Test
    fun `without data the dialog shows its initial state`() {
        val provider = TestDialogProvider(fragmentManager, "custom")
        provider.show(null)
        val dialog = provider.shown("custom")
        assertEquals("initial", dialog.data.title)
        assertEquals("initial", dialog.setups.single().title)
    }

    @Test
    fun `hiding without dialog does nothing`() {
        val provider = TestDialogProvider(fragmentManager)
        provider.hide()
        assertFalse(provider.isVisible)
    }

    @Test
    fun `the back key dismisses a cancelable dialog and notifies the listener`() {
        val provider = TestDialogProvider(fragmentManager)
        val listener = RecordingDialogListener()
        provider.dialogListener = listener
        provider.show(TestDialogData(isModal = false))
        val dialog = provider.shown()

        dialog.dialog!!.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK))
        idleMain()

        assertEquals("back", listener.calls.first())
        assertFalse(provider.isVisible)
    }

    @Test
    fun `the back key is ignored when the back button is disabled`() {
        val provider = TestDialogProvider(fragmentManager)
        val listener = RecordingDialogListener()
        provider.dialogListener = listener
        provider.show(TestDialogData(isModal = true))
        val dialog = provider.shown()

        dialog.dialog!!.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK))
        dialog.backDisabled = true
        dialog.dialog!!.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK))
        dialog.backDisabled = false
        dialog.dialog!!.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK))
        dialog.dialog!!.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))

        assertTrue(listener.calls.isEmpty())
        assertFalse(dialog.isCancelable)
    }

    @Test
    fun `cancelling the dialog notifies an outside press`() {
        val provider = TestDialogProvider(fragmentManager)
        val listener = RecordingDialogListener()
        provider.dialogListener = listener
        provider.show(TestDialogData(isModal = false))
        val dialog = provider.shown()

        dialog.dialog!!.cancel()
        idleMain()

        assertEquals("back", listener.calls.first())
    }

    @Test
    fun `the data of a dialog that is not shown can be updated`() {
        val dialog = TestDialog()
        assertEquals("changed", dialog.updateData { copy(title = "changed") }.title)
        assertTrue(dialog.setups.isEmpty())
    }
}

class TestLayout : EmaLayout<TestBinding, String> {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    companion object {
        var readAttributes = true
    }

    // Without initializer, because it is assigned while the parent constructor runs
    lateinit var attributeText: String
    val hasAttributeText get() = ::attributeText.isInitialized
    val setups = mutableListOf<String>()
    var created = false

    override fun createInitialState() = "initial"

    override fun setupAttributes(ta: TypedArray) {
        attributeText = ta.getString(0).orEmpty()
    }

    override fun getAttributes(): IntArray? = if (readAttributes) intArrayOf(android.R.attr.text) else null

    override fun onViewCreated() {
        created = true
    }

    override fun TestBinding.setup(data: String) {
        setups += data
        text.text = data
    }

    override fun createViewBinding(inflater: LayoutInflater, container: ViewGroup?) = TestBinding(inflater.context)
}

@RunWith(RobolectricTestRunner::class)
class EmaLayoutTest {

    private val activity = Robolectric.buildActivity(HostActivity::class.java).setup().get()

    private fun attributes() = Robolectric.buildAttributeSet().addAttribute(android.R.attr.text, "hello").build()

    @Test
    fun `the layout is set up with its data when it is attached`() {
        val layout = TestLayout(activity)
        assertNotNull(layout.binding)
        assertFalse(layout.viewsSetup)
        assertFalse(layout.hasAttributeText)

        activity.setContentView(layout)
        idleMain()

        assertTrue(layout.created)
        assertTrue(layout.viewsSetup)
        assertEquals(listOf("initial"), layout.setups)

        assertEquals("initial!", layout.updateData { "$this!" })
        assertEquals("initial!", layout.setups.last())
        layout.data = "direct"
        assertEquals("direct", layout.data)
    }

    @Test
    fun `the custom attributes are read when they are declared`() {
        TestLayout.readAttributes = true
        assertEquals("hello", TestLayout(activity, attributes()).attributeText)
        assertEquals("hello", TestLayout(activity, attributes(), 0).attributeText)

        TestLayout.readAttributes = false
        assertFalse(TestLayout(activity, attributes()).hasAttributeText)
        TestLayout.readAttributes = true
    }
}

@RunWith(RobolectricTestRunner::class)
class EmaEditTextTest {

    private val activity = Robolectric.buildActivity(HostActivity::class.java).setup().get()

    @Test
    fun `the text is only set when it changes`() {
        val editText = EmaEditText(activity)
        val received = mutableListOf<String?>()
        editText.setEmaTextWatcherListener { received += it }

        editText.setText("hello")
        editText.setText("hello")

        assertEquals("hello", editText.text.toString())
        assertEquals(listOf<String?>("hello"), received)
    }

    @Test
    fun `a new listener replaces the previous one`() {
        val editText = EmaEditText(activity, null)
        val first = mutableListOf<String?>()
        val second = mutableListOf<String?>()
        editText.setEmaTextWatcherListener { first += it }
        editText.setEmaTextWatcherListener { second += it }

        editText.setText("bye")

        assertTrue(first.isEmpty())
        assertEquals(listOf<String?>("bye"), second)
        assertNotNull(EmaEditText(activity, null, 0))
        assertNotNull(EmaEditText(activity, null, 0, 0))
    }

    @Test
    fun `the watcher does not notify the changes made by its own action`() {
        val editText = EditText(activity)
        var calls = 0
        val watcher = EmaTextWatcher(editText) {
            calls++
            editText.setText(it?.uppercase())
        }

        editText.setText("abc")
        assertEquals("ABC", editText.text.toString())
        assertEquals(1, calls)

        watcher.removeListener()
        editText.setText("removed")
        assertEquals(1, calls)

        watcher.restoreListener()
        editText.setText("restored")
        assertEquals("RESTORED", editText.text.toString())
        assertEquals(2, calls)
    }
}
