package com.carmabs.emanate.ui.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.carmabs.emanate.compose.nav2.EmaInitializerSupport
import com.carmabs.emanate.compose.nav2.emaScreen
import com.carmabs.emanate.compose.nav2.routeId
import com.carmabs.emanate.presentation.extension.fullName
import com.carmabs.emanate.presentation.home.HomeInitializer
import com.carmabs.emanate.presentation.home.HomeViewModel
import com.carmabs.emanate.presentation.login.LoginEvent
import com.carmabs.emanate.presentation.login.LoginViewModel
import com.carmabs.emanate.presentation.splash.SplashViewModel
import com.carmabs.emanate.sample.ui.R
import com.carmabs.emanate.ui.home.HomeScreenContent
import com.carmabs.emanate.ui.login.LoginScreenContent
import com.carmabs.emanate.ui.splash.SplashScreenContent
import com.carmabs.emanate.ui.theme.EmaSampleTheme
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get

/**
 * Launcher of the sample: the graph of the splash, the login and the home
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            EmaSampleTheme {
                val navController = rememberNavController()
                val navigator = remember { MainNavigator(navController, this) }
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    NavHost(
                        modifier = Modifier.fillMaxSize(),
                        navController = navController,
                        startDestination = SplashScreenContent::class.routeId
                    ) {
                        emaScreen(
                            screenContent = SplashScreenContent(),
                            viewModel = { get<SplashViewModel>() },
                            onEvent = { navigator.handleSplashEvent(it) }
                        )
                        emaScreen(
                            screenContent = LoginScreenContent(),
                            viewModel = { get<LoginViewModel>() },
                            onEvent = { event ->
                                navigator.handleLoginEvent(event) {
                                    val message = when (it) {
                                        is LoginEvent.Message -> getString(R.string.login_welcome, it.userName)
                                        is LoginEvent.LastUserAdded ->
                                            getString(R.string.login_last_user_added, it.user.fullName)
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
                            onEvent = { navigator.handleHomeEvent(it) }
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
