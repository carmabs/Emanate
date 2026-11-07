package com.carmabs.emanate.ui.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.splashscreen.SplashScreenViewProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.carmabs.emanate.compose.nav2.EmaInitializerSupport
import com.carmabs.emanate.compose.nav2.emaScreen
import com.carmabs.emanate.compose.nav2.routeId
import com.carmabs.emanate.presentation.extension.fullName
import com.carmabs.emanate.presentation.home.HomeEvent
import com.carmabs.emanate.presentation.home.HomeInitializer
import com.carmabs.emanate.presentation.home.HomeViewModel
import com.carmabs.emanate.presentation.login.LoginEvent
import com.carmabs.emanate.presentation.login.LoginViewModel
import com.carmabs.emanate.presentation.splash.SplashViewModel
import com.carmabs.emanate.sample.ui.R
import com.carmabs.emanate.ui.home.HomeScreenContent
import com.carmabs.emanate.ui.login.LoginScreenContent
import com.carmabs.emanate.ui.splash.SplashScreenContent
import com.carmabs.emanate.ui.environment.AppEnvironment
import com.carmabs.emanate.ui.theme.EmaSampleTheme
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get

/**
 * Launcher of the sample: the graph of the splash, the login and the home
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // The system splash is replaced by the splash screen of the app, which draws the same logo at the same place
        val splashScreen = installSplashScreen()
        var logoDrawn = false
        var systemSplash: SplashScreenViewProvider? = null
        fun removeSystemSplash() {
            systemSplash?.remove()
            systemSplash = null
        }
        // When the app is ready the system splash doesn't fade: it stays until the logo of the splash screen of the app
        // is on the screen, and then it disappears at once, so the logo looks like the same one. It must not hold the
        // first frame of the app (setKeepOnScreenCondition), because then the logo could never be drawn
        splashScreen.setOnExitAnimationListener { provider ->
            systemSplash = provider
            if (logoDrawn) removeSystemSplash()
            // The logo must be drawn in a moment. If it isn't, the app can't stay hidden behind the system splash
            provider.view.postDelayed(::removeSystemSplash, SPLASH_TIMEOUT_MS)
        }
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // The flavor of the app tells which environment is running
        val environment = get<AppEnvironment>()
        setContent {
            EmaSampleTheme {
                val navController = rememberNavController()
                val navigator = remember { MainNavigator(navController, this) }
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()

                // The surface gives the screens the background and the color of the texts, also in dark mode
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        NavHost(
                            modifier = Modifier.fillMaxSize(),
                            navController = navController,
                            startDestination = SplashScreenContent::class.routeId
                        ) {
                            emaScreen(
                                screenContent = SplashScreenContent(environment, onLogoDrawn = {
                                    logoDrawn = true
                                    removeSystemSplash()
                                }),
                                viewModel = { get<SplashViewModel>() },
                                onEvent = { navigator.handleSplashEvent(it) }
                            )
                            emaScreen(
                                screenContent = LoginScreenContent(environment),
                                viewModel = { get<LoginViewModel>() },
                                onEvent = { event ->
                                    navigator.handleLoginEvent(event) {
                                        val message = when (it) {
                                            is LoginEvent.Message -> getString(R.string.login_welcome, it.userName)
                                            is LoginEvent.LoginSuccess -> return@handleLoginEvent
                                        }
                                        scope.launch { snackbarHostState.showSnackbar(message) }
                                    }
                                }
                            )
                            emaScreen(
                                screenContent = HomeScreenContent(),
                                viewModel = { get<HomeViewModel>() },
                                initializerSupport = EmaInitializerSupport.fromRoute(HomeInitializer.serializer()),
                                onEvent = { event ->
                                    navigator.handleHomeEvent(event) {
                                        val message = when (it) {
                                            is HomeEvent.UserCreated ->
                                                getString(R.string.home_user_created, it.user.fullName)
                                            HomeEvent.CreateClicked,
                                            HomeEvent.CloseClicked -> return@handleHomeEvent
                                        }
                                        scope.launch { snackbarHostState.showSnackbar(message) }
                                    }
                                }
                            )
                        }
                        SnackbarHost(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .imePadding(),
                            hostState = snackbarHostState
                        )
                    }
                }
            }
        }
    }

    private companion object {
        const val SPLASH_TIMEOUT_MS = 1000L
    }
}
