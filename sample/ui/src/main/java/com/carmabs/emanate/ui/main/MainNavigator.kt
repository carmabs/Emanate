package com.carmabs.emanate.ui.main

import android.content.Context
import android.content.Intent
import androidx.navigation.NavController
import com.carmabs.emanate.android.initializer.EmaInitializerBundle
import com.carmabs.emanate.android.initializer.bundle.strategy.BundleSerializerStrategy
import com.carmabs.emanate.compose.nav2.navigate
import com.carmabs.emanate.compose.nav2.navigateBack
import com.carmabs.emanate.compose.nav2.routeId
import com.carmabs.emanate.presentation.home.HomeEvent
import com.carmabs.emanate.presentation.home.HomeInitializer
import com.carmabs.emanate.presentation.login.LoginEvent
import com.carmabs.emanate.presentation.splash.SplashEvent
import com.carmabs.emanate.ui.home.HomeScreenContent
import com.carmabs.emanate.ui.login.LoginScreenContent
import com.carmabs.emanate.ui.profile.ProfileActivity
import com.carmabs.emanate.ui.splash.SplashScreenContent

/**
 * Navigation of the main graph: splash, login and home
 */
class MainNavigator(private val navController: NavController, private val context: Context) {

    fun handleSplashEvent(event: SplashEvent) {
        when (event) {
            SplashEvent.SplashFinished -> navController.navigate(LoginScreenContent::class.routeId) {
                popUpTo(SplashScreenContent::class.routeId) { inclusive = true }
            }
        }
    }

    /**
     * The messages are not navigation: they are shown by [onMessage]
     */
    fun handleLoginEvent(event: LoginEvent, onMessage: (LoginEvent) -> Unit) {
        when (event) {
            is LoginEvent.LoginSuccess -> navController.navigate(
                route = HomeScreenContent::class.routeId,
                initializerBundle = EmaInitializerBundle(
                    HomeInitializer.HomeUser(event.user),
                    BundleSerializerStrategy.kSerialization(HomeInitializer.serializer())
                )
            )

            is LoginEvent.Message -> onMessage(event)
        }
    }

    /**
     * The messages are not navigation: they are shown by [onMessage]
     */
    fun handleHomeEvent(event: HomeEvent, onMessage: (HomeEvent) -> Unit) {
        when (event) {
            // The profile creation is in its own activity
            HomeEvent.CreateClicked -> context.startActivity(Intent(context, ProfileActivity::class.java))

            // The login is the screen below the home
            HomeEvent.CloseClicked -> navController.navigateBack()

            is HomeEvent.FriendCreated -> onMessage(event)
        }
    }
}
