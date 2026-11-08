package com.carmabs.emanate.ui.profile

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.carmabs.emanate.compose.nav2.emaScreen
import com.carmabs.emanate.compose.nav2.routeId
import com.carmabs.emanate.compose.ui.EmaScreenContent
import com.carmabs.emanate.core.action.EmaAction
import com.carmabs.emanate.core.action.EmaActionDispatcher
import com.carmabs.emanate.core.savestate.EmaStateStore
import com.carmabs.emanate.core.state.EmaState
import com.carmabs.emanate.presentation.profile.creation.ProfileCreationViewModel
import com.carmabs.emanate.ui.profile.creation.ProfileCreationScreenContent

/**
 * Root of the profile: the graph with the creation of a friend.
 *
 * Like the main one, it is a [EmaScreenContent] without view model, drawn directly with [onState] and not with EmaScreen.
 *
 * The view model arrives as a factory in the constructor instead of being taken from Koin here, so this class doesn't
 * depend on the injection (the activity does) and can be created with a fake one in a preview or a test. It is a
 * factory because the view model is created when its entry enters the back stack, and it needs the store of that entry.
 */
class ProfileScreenContent(
    /**
     * Factory of the view model of the creation, which receives the store of its entry
     */
    private val profileCreationViewModel: (EmaStateStore) -> ProfileCreationViewModel
) : EmaScreenContent<EmaState.EMPTY, EmaAction.EMPTY> {

    @Composable
    @SuppressLint("ComposableNaming")
    override fun onState(state: EmaState.EMPTY, actions: EmaActionDispatcher<EmaAction.EMPTY>) {
        val navController = rememberNavController()
        val navigator = remember { ProfileNavigator(navController) }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            NavHost(
                navController = navController,
                startDestination = ProfileCreationScreenContent::class.routeId
            ) {
                emaScreen(
                    screenContent = ProfileCreationScreenContent(),
                    viewModel = profileCreationViewModel,
                    onEvent = { navigator.handleProfileCreationEvent(it) }
                )
            }
        }
    }
}
