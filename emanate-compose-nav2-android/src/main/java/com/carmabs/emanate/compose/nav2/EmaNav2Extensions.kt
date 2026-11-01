package com.carmabs.emanate.compose.nav2

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
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
import com.carmabs.emanate.android.initializer.EmaInitializerBundle
import com.carmabs.emanate.android.initializer.bundle.strategy.BundleSerializerStrategy
import com.carmabs.emanate.android.savestate.EmaSaveStateManager
import com.carmabs.emanate.android.savestate.SavedStateSupport
import com.carmabs.emanate.compose.ui.EmaScreen
import com.carmabs.emanate.compose.ui.EmaScreenContent
import com.carmabs.emanate.core.action.EmaAction
import com.carmabs.emanate.core.initializer.EmaInitializer
import com.carmabs.emanate.core.savestate.EmaStateStore
import com.carmabs.emanate.core.state.EmaEvent
import com.carmabs.emanate.core.state.EmaState
import com.carmabs.emanate.core.viewmodel.EmaViewModelContract
import kotlin.reflect.KClass

/**
 * Route of the destination of a screen: unique per screen content class.
 */
val <T : EmaScreenContent<*, *>> KClass<T>.routeId: String
    get() = "screen/${this.java.name}"

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
 * @param initializerSupport where to read the initializer of the screen from: its route and, if it has none, the origin of the support
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
    val context = LocalContext.current
    val initializer = initializerSupport?.read(backEntry.arguments, context)

    EmaScreen(
        initializer = initializer,
        vm = viewModel,
        screenContent = screenContent,
        onEvent = onEvent,
        // We use the savedStateHandle of the backEntry because the one of the ViewModel is attached to the
        // navController and is restarted when the process is killed. With the one of the backEntry the data is
        // persisted correctly
        saveStateSupport = SavedStateSupport(
            backEntry.savedStateHandle,
            saveStateManager ?: EmaSaveStateManager { _, _, _ -> }
        )
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
