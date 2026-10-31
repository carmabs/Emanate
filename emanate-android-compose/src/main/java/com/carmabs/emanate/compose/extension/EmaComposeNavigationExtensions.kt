package com.carmabs.emanate.compose.extension

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.Navigator
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import com.carmabs.emanate.android.extension.findComponentActivity
import com.carmabs.emanate.android.extension.getInitializer
import com.carmabs.emanate.android.initializer.EmaInitializerBundle
import com.carmabs.emanate.android.initializer.bundle.strategy.BundleSerializerStrategy
import com.carmabs.emanate.android.savestate.EmaSaveStateManager
import com.carmabs.emanate.compose.action.EmaImmutableActionDispatcher
import com.carmabs.emanate.compose.action.toImmutable
import com.carmabs.emanate.compose.initializer.EmaInitializerSupport
import com.carmabs.emanate.compose.navigation.EmaAnimTransition
import com.carmabs.emanate.compose.navigation.EmaInitializerNavType
import com.carmabs.emanate.compose.provider.EmaScreenProvider
import com.carmabs.emanate.compose.ui.EmaScreen
import com.carmabs.emanate.compose.ui.EmaScreenContent
import com.carmabs.emanate.core.action.EmaAction
import com.carmabs.emanate.core.initializer.EmaInitializer
import com.carmabs.emanate.core.savestate.EmaStateStore
import com.carmabs.emanate.core.state.EmaEvent
import com.carmabs.emanate.core.state.EmaState
import com.carmabs.emanate.core.viewmodel.EmaViewModelContract

fun NavController.navigate(
    route: String,
    initializerBundle: EmaInitializerBundle?,
    navOptionsBuilder: (NavOptionsBuilder.() -> Unit)
) {
    val routeParsed = routeWithInitializer(route, initializerBundle)
    navigate(routeParsed, navOptionsBuilder)
}

fun NavController.navigate(
    route: String,
    initializerBundle: EmaInitializerBundle?,
    navOptions: NavOptions? = null,
    navigatorExtras: Navigator.Extras? = null
) {
    val routeParsed = routeWithInitializer(route, initializerBundle)
    navigate(routeParsed, navOptions, navigatorExtras)
}

fun <S : EmaState, A : EmaAction, E : EmaEvent> NavGraphBuilder.emaScreen(
    screenContent: EmaScreenContent<S, A>,
    viewModel: (store: EmaStateStore) -> EmaViewModelContract<S, E>,
    routeId: String = screenContent::class.routeId,
    initializerSupport: EmaInitializerSupport? = null,
    saveStateManager: EmaSaveStateManager<S, E>? = null,
    onViewModelInstance: (@Composable (EmaViewModelContract<S, E>) -> Unit)? = null,
    fullScreenDialogMode: Boolean = false,
    transitionAnimation: EmaAnimTransition = EmaAnimTransition(),
    decoration: @Composable (
        (
            content: @Composable () -> Unit,
            dispatcher: EmaImmutableActionDispatcher<A>
        ) -> Unit
    )? = null,
    previewRenderState: S? = null,
    onEvent: (E) -> Unit
) {
    val content: @Composable (NavGraphBuilder.(NavBackStackEntry) -> Unit) =
        @Composable { backEntry ->

            // We use savedStateHandle of backEntry due to SavedStateHandled of viewmodel is attached to
            // navController and is restarted on kill process.
            // With backEntry.savedStateHandle data is rightly persisted
            val androidVm = EmaScreenProvider.provideComposableViewModel(
                key = EmaScreenProvider.viewModelKey(screenContent),
                viewModel = viewModel,
                backEntry.savedStateHandle
            )

            val vm = androidVm.emaViewModel

            val vmActions = remember {
                vm.asActionDispatcher<A>().toImmutable()
            }

            val initializer = initializerSupport?.let {
                it.overrideInitializer ?: backEntry.arguments?.getInitializer(it.serializerStrategy)
            }

            LaunchedEffect(key1 = Unit) {
                saveStateManager?.onSaveStateHandling(
                    androidVm.viewModelScope,
                    androidVm.savedStateHandle,
                    androidVm.emaViewModel
                )
            }
            onViewModelInstance?.invoke(vm)
            val screenToDraw = @Composable {
                EmaScreen(
                    initializer = initializer,
                    vm = vm,
                    actions = vmActions,
                    screenContent = screenContent,
                    previewRenderState = previewRenderState,
                    onEvent = onEvent
                )
            }
            decoration?.also {
                it.invoke(screenToDraw, vmActions)
            } ?: also {
                screenToDraw()
            }
        }
    if (fullScreenDialogMode) {
        dialog(route = routeId, dialogProperties = DialogProperties()) {
            content(it)
        }
    } else {
        composable(
            route = parseRouteWithInitializerSupport(routeId),
            arguments = listOf(
                navArgument(EmaInitializer.KEY) {
                    initializerSupport?.also {
                        type = EmaInitializerNavType(it.serializerStrategy)
                    }
                    nullable = true
                }
            ),
            enterTransition = transitionAnimation.enterTransition,
            exitTransition = transitionAnimation.exitTransition,
            popEnterTransition = transitionAnimation.popEnterTransition,
            popExitTransition = transitionAnimation.popExitTransition
        ) {
            content(it)
        }
    }
}

fun routeWithInitializer(route: String, initializerBundle: EmaInitializerBundle?) = initializerBundle?.let {
    routeWithInitializer(route, it.initializer, it.serializer)
} ?: route

private fun parseRouteWithInitializerSupport(routeId: String): String =
    "$routeId?${EmaInitializer.KEY}={${EmaInitializer.KEY}}"

fun routeWithInitializer(routeId: String, initializer: EmaInitializer, serializer: BundleSerializerStrategy): String {
    // Encoded, because the serialized initializer can contain characters with meaning in a route, like & or #
    return "$routeId?${EmaInitializer.KEY}=${Uri.encode(serializer.toStringValue(initializer))}"
}

fun NavController.navigateBack(closeActivityWhenBackstackIsEmpty: Boolean = true): Boolean {
    val hasMoreBackScreens = popBackStack()
    if (!hasMoreBackScreens && closeActivityWhenBackstackIsEmpty) {
        this.context.findComponentActivity().finish()
    }

    return hasMoreBackScreens
}

fun NavController.navigateToExternalLink(url: String): Boolean = kotlin.runCatching {
    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
    context.findComponentActivity().startActivity(intent)
}.map { true }.getOrElse { false }
