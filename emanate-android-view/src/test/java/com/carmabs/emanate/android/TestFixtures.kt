package com.carmabs.emanate.android

import android.content.Context
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import com.carmabs.emanate.android.navigation.EmaActivityBackDelegate
import com.carmabs.emanate.core.initializer.EmaInitializer
import com.carmabs.emanate.core.model.EmaBackHandlerStrategy
import com.carmabs.emanate.core.state.EmaEvent
import com.carmabs.emanate.core.state.EmaState
import com.carmabs.emanate.core.viewmodel.EmaViewModelClassic
import java.io.Serializable
import kotlin.test.fail
import org.robolectric.Shadows.shadowOf

data class ViewState(val count: Int = 0, val name: String = "") : EmaState

sealed interface ViewEvent : EmaEvent {
    data object Done : ViewEvent
}

data class NameInitializer(val name: String) :
    EmaInitializer,
    Serializable

class ViewTestViewModel : EmaViewModelClassic<ViewState, ViewEvent>(ViewState()) {
    val hooks = mutableListOf<String>()
    var initializer: EmaInitializer? = null

    override fun onStateCreated(initializer: EmaInitializer?) {
        hooks += "created"
        this.initializer = initializer
    }

    override fun onViewStarted() {
        hooks += "started"
    }

    override fun onViewResumed() {
        hooks += "resumed"
    }

    override fun onViewPaused() {
        hooks += "paused"
    }

    override fun onViewStopped() {
        hooks += "stopped"
    }

    fun increment() = updateState { copy(count = count + 1) }

    fun done() = publish(ViewEvent.Done)
}

/**
 * ViewBinding written by hand, so the tests do not need layout resources.
 */
class TestBinding(context: Context) : ViewBinding {
    val text = TextView(context)
    private val rootView = FrameLayout(context).apply { addView(text) }
    override fun getRoot(): View = rootView
}

class TextBinding(context: Context) : ViewBinding {
    val text = TextView(context)
    override fun getRoot(): View = text
}

const val TEST_THEME = com.google.android.material.R.style.Theme_MaterialComponents_Light_NoActionBar

/**
 * Plain activity that hosts the fragments of the tests.
 */
open class HostActivity : AppCompatActivity() {
    val containerId = View.generateViewId()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(TEST_THEME)
        super.onCreate(savedInstanceState)
        setContentView(FrameLayout(this).apply { id = containerId })
    }
}

class DelegateOwnerActivity :
    HostActivity(),
    EmaActivityBackDelegate {
    var delegated = false
    override val ownsBackDelegate = true
    override fun onBackDelegate(): EmaBackHandlerStrategy {
        delegated = true
        return EmaBackHandlerStrategy.Cancelled
    }
}

class DelegateNotOwnerActivity :
    HostActivity(),
    EmaActivityBackDelegate {
    override val ownsBackDelegate = false
    override fun onBackDelegate(): EmaBackHandlerStrategy = EmaBackHandlerStrategy.Cancelled
}

fun idleMain() = shadowOf(Looper.getMainLooper()).idle()

/**
 * Waits for work that finishes in a background thread and posts its result to the main thread, like the
 * diff of a ListAdapter.
 */
fun waitUntil(condition: () -> Boolean) {
    repeat(400) {
        idleMain()
        if (condition()) return
        Thread.sleep(5)
    }
    fail("The condition was not met")
}
