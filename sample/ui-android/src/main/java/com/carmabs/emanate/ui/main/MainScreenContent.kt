package com.carmabs.emanate.ui.main

import android.annotation.SuppressLint
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.carmabs.emanate.compose.nav2.EmaInitializerSupport
import com.carmabs.emanate.compose.nav2.emaScreen
import com.carmabs.emanate.compose.nav2.routeId
import com.carmabs.emanate.compose.ui.EmaScreenContent
import com.carmabs.emanate.core.action.EmaAction
import com.carmabs.emanate.core.action.EmaActionDispatcher
import com.carmabs.emanate.core.state.EmaState
import com.carmabs.emanate.presentation.home.HomeInitializer
import com.carmabs.emanate.presentation.home.HomeViewModel
import com.carmabs.emanate.presentation.login.LoginViewModel
import com.carmabs.emanate.presentation.splash.SplashViewModel
import com.carmabs.emanate.ui.environment.AppEnvironment
import com.carmabs.emanate.ui.home.HomeScreenContent
import com.carmabs.emanate.ui.login.LoginScreenContent
import com.carmabs.emanate.ui.splash.SplashScreenContent
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Root of the sample: the graph of the splash, the login and the home.
 *
 * It is a [EmaScreenContent] that has no view model, so there is nothing to wrap with EmaScreen: whoever shows it calls
 * [onState] with an empty state. It shows that a screen content is only a composable that renders a state and
 * dispatches actions, and that EmaScreen is who connects it to a view model.
 *
 * The view models arrive as factories in the constructor instead of being taken from Koin here, so this class doesn't
 * depend on the injection: only the activity, which is the root of the app, knows it. That also lets it be created in
 * a preview or a test with fake view models. They are factories, not instances, because each screen creates its view
 * model when its entry enters the back stack, so it lives and dies with that entry.
 */
class MainScreenContent(
    private val environment: AppEnvironment,
    /**
     * Factories of the view models: each screen asks for its own when it enters the graph, so they belong to its entry
     */
    private val splashViewModel: () -> SplashViewModel,
    private val loginViewModel: () -> LoginViewModel,
    private val homeViewModel: () -> HomeViewModel,
    /**
     * Called when the logo of the splash screen has been drawn, so the system splash can be removed
     */
    private val onLogoDrawn: () -> Unit
) : EmaScreenContent<EmaState.EMPTY, EmaAction.EMPTY> {

    @Composable
    @SuppressLint("ComposableNaming")
    override fun onState(state: EmaState.EMPTY, actions: EmaActionDispatcher<EmaAction.EMPTY>) {
        val context = LocalContext.current
        val navController = rememberNavController()
        val navigator = remember { MainNavigator(navController, context) }
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        val messageShown = snackbarHostState.currentSnackbarData != null

        // The message goes away by itself soon, so it doesn't stay in the way of the screen
        fun showMessage(message: String) {
            scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
            }
        }

        // The surface gives the screens the background and the color of the texts, also in dark mode
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            // The message is a bar under the screen, not a floating one: the screen gets shorter while it is shown, so
            // nothing, like the create button, stays behind it
            Column(modifier = Modifier.fillMaxSize()) {
                NavHost(
                    modifier = Modifier
                        .weight(1f)
                        // The bar covers the navigation bar of the system, so the screen doesn't leave its space
                        .then(
                            if (messageShown) Modifier.consumeWindowInsets(WindowInsets.navigationBars)
                            else Modifier
                        ),
                    navController = navController,
                    startDestination = SplashScreenContent::class.routeId
                ) {
                    emaScreen(
                        screenContent = SplashScreenContent(environment, onLogoDrawn = onLogoDrawn),
                        viewModel = { splashViewModel() },
                        onEvent = { navigator.handleSplash(it) }
                    )
                    emaScreen(
                        screenContent = LoginScreenContent(environment),
                        viewModel = { loginViewModel() },
                        onEvent = { navigator.handleLogin(it, ::showMessage) }
                    )
                    emaScreen(
                        screenContent = HomeScreenContent(),
                        viewModel = { homeViewModel() },
                        initializerSupport = EmaInitializerSupport.fromRoute(HomeInitializer.serializer()),
                        onEvent = { navigator.handleHome(it, ::showMessage) }
                    )
                }
                SnackbarHost(
                    modifier = Modifier.animateContentSize(),
                    hostState = snackbarHostState
                ) { data ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.inverseSurface,
                        contentColor = MaterialTheme.colorScheme.inverseOnSurface
                    ) {
                        Text(
                            modifier = Modifier
                                .navigationBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            text = data.visuals.message,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}
