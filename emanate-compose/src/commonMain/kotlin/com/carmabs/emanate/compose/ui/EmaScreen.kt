package com.carmabs.emanate.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import com.carmabs.emanate.compose.extension.asActionDispatcher
import com.carmabs.emanate.compose.provider.EmaScreenProvider
import com.carmabs.emanate.compose.savestate.SavedStateSupport
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
        val holder = EmaScreenProvider.provideComposableViewModel(
            key = EmaScreenProvider.viewModelKey(screenContent),
            viewModel = vm,
            saveStateSupport?.savedStateHandle
        )

        val emaVm = holder.emaViewModel

        val actions = remember {
            emaVm.asActionDispatcher<A>()
        }

        LaunchedEffect(holder) {
            saveStateSupport?.saveStateManager?.onSaveStateHandling(
                holder.viewModelScope,
                holder.savedStateHandle,
                holder.emaViewModel
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

@OptIn(ExperimentalComposeUiApi::class)
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
            when (event) {
                Lifecycle.Event.ON_CREATE -> {
                    vm.onCreated(initializer)
                }

                Lifecycle.Event.ON_START -> {
                    vm.onStartView()
                }

                Lifecycle.Event.ON_RESUME -> {
                    vm.onResumeView()
                }

                Lifecycle.Event.ON_PAUSE -> {
                    vm.onPauseView()
                }

                Lifecycle.Event.ON_STOP -> {
                    vm.onStopView()
                }

                Lifecycle.Event.ON_DESTROY, Lifecycle.Event.ON_ANY -> Unit
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
