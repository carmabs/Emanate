package com.carmabs.emanate.compose.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.carmabs.emanate.compose.savestate.EmaSaveStateManager
import com.carmabs.emanate.compose.savestate.SavedStateSupport
import com.carmabs.emanate.core.initializer.EmaInitializer
import com.carmabs.emanate.core.state.EmaEvent
import com.carmabs.emanate.core.state.EmaState
import com.carmabs.emanate.core.viewmodel.EmaViewModelClassic
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive

@Suppress("DEPRECATION")
private class HolderViewModel : EmaViewModelClassic<HolderState, EmaEvent.EMPTY>(HolderState()) {
    var destroyed = false
    override fun onStateCreated(initializer: EmaInitializer?) = Unit
    override fun onDestroy() {
        destroyed = true
    }

    fun currentScope() = scope
}

private data class HolderState(val value: Int = 0) : EmaState

class EmaViewModelHolderTest {

    private fun provider(store: ViewModelStore, factory: EmaViewModelFactory<*>) =
        ViewModelProvider.create(store, factory)

    @Test
    fun `the holder gives its scope to the Ema ViewModel and clears it`() {
        val store = ViewModelStore()
        val emaViewModel = HolderViewModel()
        val holder = provider(store, EmaViewModelFactory({ emaViewModel }, SavedStateHandle()))[
            emaViewModel.id, EmaViewModelHolder::class
        ]
        assertSame(emaViewModel, holder.emaViewModel)
        assertTrue(emaViewModel.currentScope().isActive)

        store.clear()
        assertTrue(emaViewModel.destroyed)
        assertFalse(emaViewModel.currentScope().isActive)
    }

    @Test
    fun `the view model is only created if the screen doesn't have it yet`() {
        val store = ViewModelStore()
        var created = 0
        val factory = EmaViewModelFactory({ HolderViewModel().also { created++ } }, SavedStateHandle())

        val first = provider(store, factory)["screen", EmaViewModelHolder::class]
        val second = provider(store, factory)["screen", EmaViewModelHolder::class]

        assertSame(first, second)
        assertEquals(1, created)
    }

    @Test
    fun `the provided saved state handle is used`() {
        val handle = SavedStateHandle(mapOf("key" to "value"))
        val holder = provider(ViewModelStore(), EmaViewModelFactory({ HolderViewModel() }, handle))[
            "with handle", EmaViewModelHolder::class
        ]
        assertSame(handle, holder.savedStateHandle)
    }

    @Test
    fun `save state support keeps the handle and the manager`() {
        val handle = SavedStateHandle()
        val manager = EmaSaveStateManager<HolderState, EmaEvent.EMPTY> { _, saveStateHandle, _ ->
            saveStateHandle["saved"] = true
        }
        val support = SavedStateSupport(handle, manager)
        support.saveStateManager.onSaveStateHandling(
            CoroutineScope(Dispatchers.Unconfined),
            support.savedStateHandle,
            HolderViewModel()
        )
        assertEquals(true, handle.get<Boolean>("saved"))
    }
}
