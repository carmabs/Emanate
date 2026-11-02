package com.carmabs.emanate.presentation.profile.onboarding

import com.carmabs.domain.model.Role
import com.carmabs.domain.model.User
import com.carmabs.emanate.core.initializer.EmaInitializer
import com.carmabs.emanate.presentation.base.BaseActionViewModel

class ProfileOnBoardingActionViewModel(initialDataState: ProfileOnBoardingState) :
    BaseActionViewModel<ProfileOnBoardingState, ProfileOnBoardingActions, ProfileOnBoardingEvent>(
        initialDataState
    ) {

    override fun onStateCreated(initializer: EmaInitializer?) {
        when (val onBoardingInitializer = initializer as ProfileOnBoardingInitializer) {
            is ProfileOnBoardingInitializer.Default -> {
                updateState {
                    copy(user = User(onBoardingInitializer.admin))
                }
            }
        }
    }

    override fun onAction(action: ProfileOnBoardingActions) {
        when (action) {
            ProfileOnBoardingActions.AdminClicked -> onActionAdminClicked()
            ProfileOnBoardingActions.UserClicked -> onActionUserClicked()
            ProfileOnBoardingActions.BackClicked -> onActionBackClicked()
        }
    }
    private fun onActionAdminClicked() {
        publish(
            ProfileOnBoardingEvent.UserTypeSelected(Role.ADMIN)
        )
    }

    private fun onActionUserClicked() {
        publish(
            ProfileOnBoardingEvent.UserTypeSelected(Role.BASIC)
        )
    }

    private fun onActionBackClicked() {
        publish(ProfileOnBoardingEvent.OnBoardingCancelled)
    }
}
