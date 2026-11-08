package com.carmabs.emanate.ui.main

import android.content.Context
import android.content.Intent
import androidx.navigation.NavController
import com.carmabs.emanate.android.initializer.EmaInitializerBundle
import com.carmabs.emanate.android.initializer.bundle.strategy.BundleSerializerStrategy
import com.carmabs.emanate.compose.nav2.navigate
import com.carmabs.emanate.compose.nav2.navigateBack
import com.carmabs.emanate.compose.nav2.routeId
import com.carmabs.emanate.presentation.extension.fullName
import com.carmabs.emanate.presentation.home.HomeEvent
import com.carmabs.emanate.presentation.home.HomeInitializer
import com.carmabs.emanate.presentation.login.LoginEvent
import com.carmabs.emanate.presentation.splash.SplashEvent
import com.carmabs.emanate.sample.ui.R
import com.carmabs.emanate.ui.home.HomeScreenContent
import com.carmabs.emanate.ui.login.LoginScreenContent
import com.carmabs.emanate.ui.profile.ProfileActivity
import com.carmabs.emanate.ui.splash.SplashScreenContent

/**
 * Navigation of the main graph: splash, login and home
 */
class MainNavigator(private val navController: NavController, private val context: Context) {

    fun handleSplash(event: SplashEvent) {
        when (event) {
            SplashEvent.SplashFinished -> navController.navigate(LoginScreenContent::class.routeId) {
                popUpTo(SplashScreenContent::class.routeId) { inclusive = true }
            }
        }
    }

    /**
     * The messages are not navigation: [onMessage] shows them
     */
    fun handleLogin(event: LoginEvent, onMessage: (String) -> Unit) {
        when (event) {
            is LoginEvent.LoginSuccess -> navController.navigate(
                route = HomeScreenContent::class.routeId,
                initializerBundle = EmaInitializerBundle(
                    HomeInitializer.HomeUser(event.user),
                    BundleSerializerStrategy.kSerialization(HomeInitializer.serializer())
                )
            )

            is LoginEvent.Message -> onMessage(context.getString(R.string.login_welcome, event.userName))
        }
    }

    /**
     * The messages are not navigation: [onMessage] shows them
     */
    fun handleHome(event: HomeEvent, onMessage: (String) -> Unit) {
        when (event) {
            // The profile creation is in its own activity
            HomeEvent.CreateClicked -> context.startActivity(Intent(context, ProfileActivity::class.java))

            // The login is the screen below the home
            HomeEvent.CloseClicked -> navController.navigateBack()

            is HomeEvent.FriendCreated -> onMessage(context.getString(R.string.home_user_created, event.friend.fullName))
            is HomeEvent.DeleteFailed -> onMessage(context.getString(R.string.home_delete_failed, event.friend.fullName))
            HomeEvent.LoadFailed -> onMessage(context.getString(R.string.home_load_failed))
        }
    }
}
