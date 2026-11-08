package com.carmabs.emanate.presentation.profile.creation

import com.carmabs.emanate.core.action.EmaAction

sealed interface ProfileCreationAction : EmaAction {

    data class UserNameWritten(val name: String) : ProfileCreationAction

    data class UserSurnameWritten(val surname: String) : ProfileCreationAction

    data object CreateClicked : ProfileCreationAction

    data object DialogConfirmClicked : ProfileCreationAction

    data object DialogCancelClicked : ProfileCreationAction

    data object OnBack : ProfileCreationAction

    data object DialogBackConfirm : ProfileCreationAction

    data object DialogBackCancel : ProfileCreationAction

    data object DialogErrorAccepted : ProfileCreationAction

    /**
     * Actions dispatched by the side effects of ProfileCreationViewModel to report what they did
     */
    sealed interface SideEffects : ProfileCreationAction {
        /**
         * The friend couldn't be stored
         */
        data object CreateFailed : SideEffects
    }
}
