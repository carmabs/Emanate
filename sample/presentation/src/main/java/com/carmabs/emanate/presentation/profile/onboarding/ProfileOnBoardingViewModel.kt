package com.carmabs.emanate.presentation.profile.onboarding

import com.carmabs.domain.model.Role
import com.carmabs.domain.model.User
import com.carmabs.emanate.core.viewmodel.EmaViewModel

class ProfileOnBoardingViewModel(initialDataState: ProfileOnBoardingState) :
    EmaViewModel<ProfileOnBoardingState, ProfileOnBoardingActions, ProfileOnBoardingEvent>(initialDataState) {

    override fun setup() = source {
        reducers {
            add<ProfileOnBoardingInitializer> { initializer ->
                when (initializer) {
                    is ProfileOnBoardingInitializer.Default -> state.copy(user = User(initializer.admin))
                }
            }
        }

        sideEffects {
            add<ProfileOnBoardingActions> { action ->
                val event = when (action) {
                    ProfileOnBoardingActions.AdminClicked -> ProfileOnBoardingEvent.UserTypeSelected(Role.ADMIN)
                    ProfileOnBoardingActions.UserClicked -> ProfileOnBoardingEvent.UserTypeSelected(Role.BASIC)
                    ProfileOnBoardingActions.BackClicked -> ProfileOnBoardingEvent.OnBoardingCancelled
                }
                publish(event)
            }
        }
    }
}
