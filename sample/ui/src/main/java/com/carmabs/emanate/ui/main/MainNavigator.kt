package com.carmabs.emanate.ui.main

import android.content.Context
import android.content.Intent
import androidx.navigation.NavController
import com.carmabs.emanate.android.initializer.EmaInitializerBundle
import com.carmabs.emanate.android.initializer.bundle.strategy.BundleSerializerStrategy
import com.carmabs.emanate.android.extension.setInitializer
import com.carmabs.emanate.compose.nav2.navigate
import com.carmabs.emanate.compose.nav2.routeId
import com.carmabs.emanate.presentation.home.HomeEvent
import com.carmabs.emanate.presentation.home.HomeInitializer
import com.carmabs.emanate.presentation.login.LoginEvent
import com.carmabs.emanate.presentation.profile.onboarding.ProfileOnBoardingInitializer
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

            is LoginEvent.Message,
            is LoginEvent.LastUserAdded -> onMessage(event)
        }
    }

    fun handleHomeEvent(event: HomeEvent) {
        when (event) {
            // The profile screens are in their own activity, which reads the initializer from its intent
            is HomeEvent.ProfileClicked -> context.startActivity(
                Intent(context, ProfileActivity::class.java).setInitializer(
                    ProfileOnBoardingInitializer.Default(event.user.name),
                    BundleSerializerStrategy.kSerialization(ProfileOnBoardingInitializer.serializer())
                )
            )
        }
    }
}
