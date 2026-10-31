package com.carmabs.emanate.ui.profile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.carmabs.emanate.android.extension.getInitializer
import com.carmabs.emanate.android.initializer.bundle.strategy.BundleSerializerStrategy
import com.carmabs.emanate.android.savestate.EmaSaveStateManager
import com.carmabs.emanate.compose.extension.asActionDispatcher
import com.carmabs.emanate.compose.extension.emaScreen
import com.carmabs.emanate.compose.extension.routeId
import com.carmabs.emanate.compose.initializer.EmaInitializerSupport
import com.carmabs.emanate.presentation.profile.creation.ProfileCreationAction
import com.carmabs.emanate.presentation.profile.creation.ProfileCreationActionViewModel
import com.carmabs.emanate.presentation.profile.creation.ProfileCreationEvent
import com.carmabs.emanate.presentation.profile.creation.ProfileCreationInitializer
import com.carmabs.emanate.presentation.profile.creation.ProfileCreationState
import com.carmabs.emanate.presentation.profile.creation.ProfileCreationViewModel
import com.carmabs.emanate.presentation.profile.onboarding.ProfileOnBoardingActionViewModel
import com.carmabs.emanate.presentation.profile.onboarding.ProfileOnBoardingInitializer
import com.carmabs.emanate.presentation.profile.onboarding.ProfileOnBoardingViewModel
import com.carmabs.emanate.ui.base.sampleViewModel
import com.carmabs.emanate.ui.profile.creation.ProfileCreationScreenContent
import com.carmabs.emanate.ui.profile.onboarding.ProfileOnBoardingScreenContent
import com.carmabs.emanate.ui.theme.EmaSampleTheme
import kotlinx.coroutines.launch

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
                        initializerSupport = EmaInitializerSupport.kSerialization(
                            ProfileOnBoardingInitializer.serializer(),
                            getInitializer(
                                BundleSerializerStrategy.kSerialization(
                                    ProfileOnBoardingInitializer.serializer()
                                ),
                                savedInstanceState
                            )
                        ),
                        screenContent = ProfileOnBoardingScreenContent(),
                        onEvent = {
                            navigator.handleProfileOnBoardingEvent(it)
                        },
                        viewModel = { store ->
                            sampleViewModel(
                                ProfileOnBoardingViewModel::class,
                                ProfileOnBoardingActionViewModel::class,
                                store
                            )
                        }
                    )
                    emaScreen(
                        screenContent = ProfileCreationScreenContent(),
                        viewModel = { store ->
                            sampleViewModel(
                                ProfileCreationViewModel::class,
                                ProfileCreationActionViewModel::class,
                                store
                            )
                        },
                        onEvent = {
                            navigator.handleProfileCreationEvent(it)
                        },
                        initializerSupport = EmaInitializerSupport.kSerialization(
                            ProfileCreationInitializer.serializer()
                        ),
                        saveStateManager = EmaSaveStateManager<ProfileCreationState, ProfileCreationEvent> {
                                coroutineScope,
                                savedStateHandle,
                                emaViewModel
                            ->
                            // ProfileCreationViewModel saves its form with a backup installed in its setup, in this
                            // same handle. EmaViewModelAction can't, so the screen saves it as before the backups.
                            if (emaViewModel !is ProfileCreationActionViewModel) return@EmaSaveStateManager

                            val keyName = "USERNAME"
                            val keySurname = "SURNAME"

                            savedStateHandle.get<String>(keyName)?.also {
                                emaViewModel.asActionDispatcher<ProfileCreationAction>()
                                    .dispatch(ProfileCreationAction.UserNameWritten(it))
                            }
                            savedStateHandle.get<String>(keySurname)?.also {
                                emaViewModel.asActionDispatcher<ProfileCreationAction>()
                                    .dispatch(ProfileCreationAction.UserSurnameWritten(it))
                            }
                            coroutineScope.launch {
                                emaViewModel.stateFlow.collect {
                                    savedStateHandle[keyName] = it.name
                                    savedStateHandle[keySurname] = it.surname
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
