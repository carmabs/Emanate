package com.carmabs.emanate.compose.ui

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import com.carmabs.emanate.android.savestate.SavedStateSupport
import com.carmabs.emanate.compose.extension.asActionDispatcher
import com.carmabs.emanate.compose.provider.EmaScreenProvider
import com.carmabs.emanate.compose.utils.Compose
import com.carmabs.emanate.core.action.EmaAction
import com.carmabs.emanate.core.action.EmaActionDispatcher
import com.carmabs.emanate.core.action.EmaEventConsumer
import com.carmabs.emanate.core.initializer.EmaInitializer
import com.carmabs.emanate.core.model.EmaBackHandlerStrategy
import com.carmabs.emanate.core.savestate.EmaStateStore
import com.carmabs.emanate.core.state.EmaEvent
import com.carmabs.emanate.core.state.EmaState
import com.carmabs.emanate.core.utils.EmaUtils
import com.carmabs.emanate.core.viewmodel.EmaViewModelContract

/**
 * Draws a view model created and kept by its caller, which also closes it. Used by the destinations of the navigation
 * graph, whose view model is kept by their back stack entry.
 */
@Composable
internal fun <S : EmaState, A : EmaAction, E : EmaEvent> EmaScreen(
    initializer: EmaInitializer? = null,
    vm: EmaViewModelContract<S, E>,
    actions: EmaActionDispatcher<A>,
    screenContent: EmaScreenContent<S, A>,
    onEvent: (E) -> Unit,
    previewRenderState: S? = null
) {
    EmaUtils.Preview.Compose.skipForPreview(
        previewComposable = {
            previewRenderState?.also { previewState ->
                screenContent.onState(
                    state = previewState,
                    actions = EmaActionDispatcher.EMPTY
                )
            }
        }
    ) {
        RenderScreen(
            initializer,
            screenContent,
            vm,
            actions,
            onEvent
        )
    }
}

@Composable
fun <A : EmaAction, S : EmaState, E : EmaEvent> EmaScreen(
    initializer: EmaInitializer? = null,
    vm: (store: EmaStateStore) -> EmaViewModelContract<S, E>,
    screenContent: EmaScreenContent<S, A>,
    onEvent: (E) -> Unit,
    saveStateSupport: SavedStateSupport<S, E>? = null,
    previewRenderState: S? = null
) {
    EmaUtils.Preview.Compose.skipForPreview(
        previewComposable = {
            previewRenderState?.also { previewState ->
                screenContent.onState(
                    state = previewState,
                    actions = EmaActionDispatcher.EMPTY
                )
            }
        }
    ) {
        val androidVm = EmaScreenProvider.provideComposableViewModel(
            key = EmaScreenProvider.viewModelKey(screenContent),
            viewModel = vm,
            saveStateSupport?.savedStateHandle
        )

        val emaVm = androidVm.emaViewModel

        val actions = remember {
            emaVm.asActionDispatcher<A>()
        }

        LaunchedEffect(androidVm) {
            saveStateSupport?.saveStateManager?.onSaveStateHandling(
                androidVm.viewModelScope,
                androidVm.savedStateHandle,
                androidVm.emaViewModel
            )
        }

        RenderScreen(
            initializer,
            screenContent,
            emaVm,
            actions,
            onEvent
        )
    }
}

@Composable
private fun <S : EmaState, A : EmaAction, E : EmaEvent> RenderScreen(
    initializer: EmaInitializer?,
    screenContent: EmaScreenContent<S, A>,
    vm: EmaViewModelContract<S, E>,
    actions: EmaActionDispatcher<A>,
    onEvent: (E) -> Unit
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            val tag = "LIFECYCLE"
            when (event) {
                Lifecycle.Event.ON_CREATE -> {
                    Log.d(tag, "On create")
                    vm.onCreated(initializer)
                }

                Lifecycle.Event.ON_START -> {
                    Log.d(tag, "On start")
                    vm.onStartView()
                }

                Lifecycle.Event.ON_RESUME -> {
                    Log.d(tag, "On resume")
                    vm.onResumeView()
                }

                Lifecycle.Event.ON_PAUSE -> {
                    Log.d(tag, "On pause")
                    vm.onPauseView()
                }

                Lifecycle.Event.ON_STOP -> {
                    Log.d(tag, "On stop")
                    vm.onStopView()
                }

                Lifecycle.Event.ON_DESTROY -> {
                    Log.d(tag, "On destroy")
                }

                Lifecycle.Event.ON_ANY -> {
                    Log.d(tag, "On any")
                }
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            vm.onPauseView()
            vm.onStopView()
            lifecycle.removeObserver(observer)
        }
    }

    val state = vm.stateFlow
        .collectAsStateWithLifecycle(initialValue = vm.initialState, lifecycle = lifecycle).value

    val eventFlow = vm.eventFlow
    LaunchedEffect(key1 = eventFlow, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            eventFlow.collect { events ->
                events.forEach { event ->
                    onEvent(event)
                    (vm as? EmaEventConsumer<E>)?.consumeEvent(event)
                }
            }
        }
    }

    val backAction = screenContent.onBack(state)
    BackHandler(enabled = backAction != null) {
        backAction?.also(actions::dispatch)
    }

    if (vm.shouldRenderState) {
        screenContent.onState(state, actions)
    }
}
