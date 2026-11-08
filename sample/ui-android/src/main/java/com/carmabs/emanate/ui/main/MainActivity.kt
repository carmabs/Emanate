package com.carmabs.emanate.ui.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.splashscreen.SplashScreenViewProvider
import com.carmabs.emanate.core.action.EmaActionDispatcher
import com.carmabs.emanate.core.state.EmaState
import com.carmabs.emanate.presentation.home.HomeViewModel
import com.carmabs.emanate.presentation.login.LoginViewModel
import com.carmabs.emanate.presentation.splash.SplashViewModel
import com.carmabs.emanate.ui.environment.AppEnvironment
import com.carmabs.emanate.ui.theme.EmaSampleTheme
import org.koin.android.ext.android.get

/**
 * Launcher of the sample: it shows [MainScreenContent], the graph of the splash, the login and the home
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
        val mainContent = MainScreenContent(
            environment = get<AppEnvironment>(),
            splashViewModel = { get<SplashViewModel>() },
            loginViewModel = { get<LoginViewModel>() },
            homeViewModel = { get<HomeViewModel>() },
            onLogoDrawn = {
                logoDrawn = true
                removeSystemSplash()
            }
        )
        setContent {
            EmaSampleTheme {
                // There is no view model behind the main content, so it is drawn directly, without EmaScreen
                mainContent.onState(EmaState.EMPTY, EmaActionDispatcher.EMPTY)
            }
        }
    }

    private companion object {
        const val SPLASH_TIMEOUT_MS = 1000L
    }
}
