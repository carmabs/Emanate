package com.carmabs.emanate.ui.profile

import androidx.navigation.NavController
import com.carmabs.emanate.compose.nav2.navigateBack
import com.carmabs.emanate.presentation.profile.creation.ProfileCreationEvent

class ProfileNavigator(private val navController: NavController) {

    // It is the only screen of the activity, so going back closes it
    fun handleProfileCreation(event: ProfileCreationEvent) {
        when (event) {
            ProfileCreationEvent.DialogConfirmationAccepted -> navController.navigateBack()
        }
    }
}
