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

/**
 * Adds the screen drawn by [screenContent] to the graph, as a composable destination.
 *
 * @param viewModel creates the ViewModel of the screen, once, with the store of its backup
 * @param routeId route of the destination, unique per screen content class by default
 * @param initializerSupport how to read the initializer of the screen from its route
 * @param saveStateManager saves the state of the screen if the process dies
 * @param transitionAnimation animations of the destination
 * @param onEvent receives every event of the screen
 */
fun <S : EmaState, A : EmaAction, E : EmaEvent> NavGraphBuilder.emaScreen(
    screenContent: EmaScreenContent<S, A>,
    viewModel: (store: EmaStateStore) -> EmaViewModelContract<S, E>,
    routeId: String = screenContent::class.routeId,
    initializerSupport: EmaInitializerSupport? = null,
    saveStateManager: EmaSaveStateManager<S, E>? = null,
    transitionAnimation: EmaAnimTransition = EmaAnimTransition(),
    onEvent: (E) -> Unit
) {
    composable(
        route = parseRouteWithInitializerSupport(routeId),
        arguments = initializerArguments(initializerSupport),
        enterTransition = transitionAnimation.enterTransition,
        exitTransition = transitionAnimation.exitTransition,
        popEnterTransition = transitionAnimation.popEnterTransition,
        popExitTransition = transitionAnimation.popExitTransition
    ) { backEntry ->
        EmaDestination(backEntry, screenContent, viewModel, initializerSupport, saveStateManager, onEvent)
    }
}

/**
 * Adds the screen drawn by [screenContent] to the graph, as a dialog destination. It works as [emaScreen], but the
 * screen is shown in a dialog over the previous one.
 *
 * @param dialogProperties properties of the dialog
 */
fun <S : EmaState, A : EmaAction, E : EmaEvent> NavGraphBuilder.emaDialog(
    screenContent: EmaScreenContent<S, A>,
    viewModel: (store: EmaStateStore) -> EmaViewModelContract<S, E>,
    routeId: String = screenContent::class.routeId,
    initializerSupport: EmaInitializerSupport? = null,
    saveStateManager: EmaSaveStateManager<S, E>? = null,
    dialogProperties: DialogProperties = DialogProperties(),
    onEvent: (E) -> Unit
) {
    dialog(
        route = parseRouteWithInitializerSupport(routeId),
        arguments = initializerArguments(initializerSupport),
        dialogProperties = dialogProperties
    ) { backEntry ->
        EmaDestination(backEntry, screenContent, viewModel, initializerSupport, saveStateManager, onEvent)
    }
}

private fun initializerArguments(initializerSupport: EmaInitializerSupport?) = listOf(
    navArgument(EmaInitializer.KEY) {
        initializerSupport?.also {
            type = EmaInitializerNavType(it.serializerStrategy)
        }
        nullable = true
    }
)

@Composable
private fun <S : EmaState, A : EmaAction, E : EmaEvent> EmaDestination(
    backEntry: NavBackStackEntry,
    screenContent: EmaScreenContent<S, A>,
    viewModel: (store: EmaStateStore) -> EmaViewModelContract<S, E>,
    initializerSupport: EmaInitializerSupport?,
    saveStateManager: EmaSaveStateManager<S, E>?,
    onEvent: (E) -> Unit
) {
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

    EmaScreen(
        initializer = initializer,
        vm = vm,
        actions = vmActions,
        screenContent = screenContent,
        onEvent = onEvent
    )
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
