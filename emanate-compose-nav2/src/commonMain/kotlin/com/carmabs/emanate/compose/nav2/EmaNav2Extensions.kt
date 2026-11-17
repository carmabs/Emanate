package com.carmabs.emanate.compose.nav2

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.Navigator
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import com.carmabs.emanate.compose.savestate.EmaSaveStateManager
import com.carmabs.emanate.compose.savestate.SavedStateSupport
import com.carmabs.emanate.compose.ui.EmaScreen
import com.carmabs.emanate.compose.ui.EmaScreenContent
import com.carmabs.emanate.core.action.EmaAction
import com.carmabs.emanate.core.initializer.EmaInitializer
import com.carmabs.emanate.core.savestate.EmaStateStore
import com.carmabs.emanate.core.state.EmaEvent
import com.carmabs.emanate.core.state.EmaState
import com.carmabs.emanate.core.viewmodel.EmaViewModelContract
import kotlin.reflect.KClass
import kotlinx.serialization.KSerializer

/**
 * Route of the destination of a screen: unique per screen content class.
 */
val <T : EmaScreenContent<*, *>> KClass<T>.routeId: String
    get() = "screen/${this.qualifiedName ?: this.simpleName}"

fun <I : EmaInitializer> NavController.navigate(
    route: String,
    initializer: I?,
    serializer: KSerializer<I>,
    navOptionsBuilder: (NavOptionsBuilder.() -> Unit)
) {
    navigate(routeWithInitializer(route, initializer, serializer), navOptionsBuilder)
}

fun <I : EmaInitializer> NavController.navigate(
    route: String,
    initializer: I?,
    serializer: KSerializer<I>,
    navOptions: NavOptions? = null,
    navigatorExtras: Navigator.Extras? = null
) {
    navigate(routeWithInitializer(route, initializer, serializer), navOptions, navigatorExtras)
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
            type = EmaInitializerNavType(it.serializer)
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
    val initializer = initializerSupport?.read(backEntry.arguments)

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

private fun parseRouteWithInitializerSupport(routeId: String): String =
    "$routeId?${EmaInitializer.KEY}={${EmaInitializer.KEY}}"

/**
 * Route of [routeId] with [initializer] as argument. Returns [routeId] when there is no initializer.
 */
fun <I : EmaInitializer> routeWithInitializer(routeId: String, initializer: I?, serializer: KSerializer<I>): String {
    if (initializer == null) return routeId
    return "$routeId?${EmaInitializer.KEY}=${percentEncode(encodeInitializer(serializer, initializer))}"
}

/**
 * Pops the current destination. Returns false, after calling [onBackStackEmpty], when there was none to go back to.
 */
fun NavController.navigateBack(onBackStackEmpty: () -> Unit = {}): Boolean {
    val hasMoreBackScreens = popBackStack()
    if (!hasMoreBackScreens) onBackStackEmpty()
    return hasMoreBackScreens
}

// Encoded, because the serialized initializer can contain characters with meaning in a route, like & or #
private fun percentEncode(value: String): String = buildString {
    for (byte in value.encodeToByteArray()) {
        val code = byte.toInt() and 0xFF
        val char = code.toChar()
        if (code < 0x80 && (char.isLetterOrDigit() || char in "-._~")) {
            append(char)
        } else {
            append('%')
            append(HEX[code shr 4])
            append(HEX[code and 0xF])
        }
    }
}

private const val HEX = "0123456789ABCDEF"
