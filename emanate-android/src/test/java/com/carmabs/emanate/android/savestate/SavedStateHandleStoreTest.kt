package com.carmabs.emanate.android.savestate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import com.carmabs.emanate.android.viewmodel.EmaAndroidViewModel
import com.carmabs.emanate.android.viewmodel.EmaViewModelFactory
import com.carmabs.emanate.core.action.EmaAction
import com.carmabs.emanate.core.concurrency.EmaDispatchers
import com.carmabs.emanate.core.model.EmaConfiguration
import com.carmabs.emanate.core.savestate.EmaStateStore
import com.carmabs.emanate.core.savestate.backupOf
import com.carmabs.emanate.core.state.EmaEvent
import com.carmabs.emanate.core.state.EmaState
import com.carmabs.emanate.core.viewmodel.EmaViewModel
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlinx.coroutines.Dispatchers
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

private data class NameState(val name: String = "") : EmaState

private sealed interface NameAction : EmaAction {
    data class Written(val name: String) : NameAction
}

private class NameViewModel(private val store: EmaStateStore) :
    EmaViewModel<NameState, NameAction, EmaEvent.EMPTY>(
        NameState(),
        EmaConfiguration(dispatchers = EmaDispatchers(main = Dispatchers.Unconfined), logger = null)
    ) {

    override fun setup() = source {
        plugins {
            add(
                backupOf(store) {
                    add("name", save = { name.takeIf { it.isNotEmpty() } }, restore = { copy(name = it) })
                }
            )
        }
        reducers {
            add<NameAction.Written> { action -> state.copy(name = action.name) }
        }
    }
}

@RunWith(RobolectricTestRunner::class)
class SavedStateHandleStoreTest {

    @Test
    fun `the values are saved in the handle with a prefix`() {
        val handle = SavedStateHandle(mapOf("name" to "argument"))
        val store = SavedStateHandleStore(handle)

        store.write("name", "Ana")

        assertEquals("Ana", store.read("name"))
        assertEquals("Ana", handle.get<String>("emanate.saved.name"))
        assertEquals("argument", handle.get<String>("name"))
    }

    @Test
    fun `a null value removes the saved one`() {
        val handle = SavedStateHandle()
        val store = SavedStateHandleStore(handle)
        store.write("name", "Ana")

        store.write("name", null)

        assertNull(store.read("name"))
        assertFalse(handle.contains("emanate.saved.name"))
    }

    @Test
    fun `the android view model saves the state of the ema view model in its handle and restores it`() {
        val handle = SavedStateHandle()
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val first = androidViewModel(activity, handle, "first")
        first.emaViewModel.onCreated(null)
        (first.emaViewModel as NameViewModel).dispatch(NameAction.Written("Ana"))

        // The process dies: a new view model is created with the restored handle
        val restoredHandle = SavedStateHandle(handle.keys().associateWith { handle.get<Any>(it) })
        val second = androidViewModel(activity, restoredHandle, "second")
        second.emaViewModel.onCreated(null)

        assertEquals(NameState("Ana"), second.emaViewModel.stateFlow.value)
    }

    @Test
    fun `the state is restored when the activity is recreated from its saved state`() {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val first = androidViewModel(controller.get())
        first.emaViewModel.onCreated(null)
        (first.emaViewModel as NameViewModel).dispatch(NameAction.Written("Ana"))
        val outState = Bundle()
        controller.pause().stop().saveInstanceState(outState)

        // The process dies: the activity is created again from its saved state, with a new store of view models
        val restoredActivity = Robolectric.buildActivity(ComponentActivity::class.java).setup(outState).get()
        val second = androidViewModel(restoredActivity)
        second.emaViewModel.onCreated(null)

        assertEquals(NameState("Ana"), second.emaViewModel.stateFlow.value)
    }

    /**
     * Like a Fragment or an Activity of emanate-android-view, without giving a handle
     */
    @Suppress("UNCHECKED_CAST")
    private fun androidViewModel(activity: ComponentActivity) =
        ViewModelProvider(activity, EmaViewModelFactory(::NameViewModel))["name", EmaAndroidViewModel::class.java]
            as EmaAndroidViewModel<NameState, EmaEvent.EMPTY>

    @Suppress("UNCHECKED_CAST")
    private fun androidViewModel(activity: ComponentActivity, handle: SavedStateHandle, key: String) =
        ViewModelProvider(activity, EmaViewModelFactory(::NameViewModel, handle))[key, EmaAndroidViewModel::class.java]
            as EmaAndroidViewModel<NameState, EmaEvent.EMPTY>
}
