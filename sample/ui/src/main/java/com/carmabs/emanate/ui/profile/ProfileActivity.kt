package com.carmabs.emanate.ui.profile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import org.koin.android.ext.android.get
import org.koin.core.parameter.parametersOf
import com.carmabs.emanate.compose.nav2.emaScreen
import com.carmabs.emanate.compose.nav2.routeId
import com.carmabs.emanate.compose.nav2.EmaInitializerSupport
import com.carmabs.emanate.presentation.profile.creation.ProfileCreationInitializer
import com.carmabs.emanate.presentation.profile.creation.ProfileCreationViewModel
import com.carmabs.emanate.presentation.profile.onboarding.ProfileOnBoardingInitializer
import com.carmabs.emanate.presentation.profile.onboarding.ProfileOnBoardingViewModel
import com.carmabs.emanate.ui.profile.creation.ProfileCreationScreenContent
import com.carmabs.emanate.ui.profile.onboarding.ProfileOnBoardingScreenContent
import com.carmabs.emanate.ui.theme.EmaSampleTheme

class ProfileActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            EmaSampleTheme {
                val navController = rememberNavController()
                val navigator = remember {
                    ProfileNavigator(navController)
                }

                NavHost(
                    navController = navController,
                    startDestination = ProfileOnBoardingScreenContent::class.routeId
                ) {
                    emaScreen(
                        // The home screen opens this activity with the initializer in its intent
                        initializerSupport = EmaInitializerSupport.fromIntent(
                            ProfileOnBoardingInitializer.serializer()
                        ),
                        screenContent = ProfileOnBoardingScreenContent(),
                        onEvent = {
                            navigator.handleProfileOnBoardingEvent(it)
                        },
                        viewModel = { get<ProfileOnBoardingViewModel>() }
                    )
                    emaScreen(
                        screenContent = ProfileCreationScreenContent(),
                        viewModel = { store -> get<ProfileCreationViewModel> { parametersOf(store) } },
                        onEvent = {
                            navigator.handleProfileCreationEvent(it)
                        },
                        initializerSupport = EmaInitializerSupport.fromRoute(
                            ProfileCreationInitializer.serializer()
                        )
                    )
                }
            }
        }
    }
}
