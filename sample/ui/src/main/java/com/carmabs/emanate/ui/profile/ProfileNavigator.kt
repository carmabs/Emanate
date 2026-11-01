package com.carmabs.emanate.ui.profile

import androidx.navigation.NavController
import com.carmabs.domain.model.Role
import com.carmabs.emanate.android.initializer.EmaInitializerBundle
import com.carmabs.emanate.android.initializer.bundle.strategy.BundleSerializerStrategy
import com.carmabs.emanate.compose.nav2.navigate
import com.carmabs.emanate.compose.nav2.navigateBack
import com.carmabs.emanate.compose.nav2.routeId
import com.carmabs.emanate.presentation.profile.creation.ProfileCreationEvent
import com.carmabs.emanate.presentation.profile.creation.ProfileCreationInitializer
import com.carmabs.emanate.presentation.profile.onboarding.ProfileOnBoardingEvent
import com.carmabs.emanate.ui.profile.creation.ProfileCreationScreenContent

class ProfileNavigator(private val navController: NavController) {
    fun handleProfileOnBoardingEvent(event: ProfileOnBoardingEvent) {
        when (event) {
            is ProfileOnBoardingEvent.UserTypeSelected -> {
                navController.navigate(
                    route = ProfileCreationScreenContent::class.routeId,
                    initializerBundle = EmaInitializerBundle(
                        mapToCreationInitializer(event.role),
                        BundleSerializerStrategy.kSerialization(ProfileCreationInitializer.serializer())
                    )
                )
            }

            ProfileOnBoardingEvent.OnBoardingCancelled -> navController.navigateBack()
        }
    }

    private fun mapToCreationInitializer(role: Role) = when (role) {
        Role.ADMIN -> ProfileCreationInitializer.Admin
        Role.BASIC -> ProfileCreationInitializer.UserBasic
    }

    fun handleProfileCreationEvent(event: ProfileCreationEvent) {
        when (event) {
            ProfileCreationEvent.DialogConfirmationAccepted -> navController.navigateBack()
        }
    }
}
