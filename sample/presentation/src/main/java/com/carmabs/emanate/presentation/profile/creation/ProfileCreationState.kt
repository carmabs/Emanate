package com.carmabs.emanate.presentation.profile.creation

import com.carmabs.emanate.core.constants.STRING_EMPTY
import com.carmabs.emanate.core.state.EmaState

data class ProfileCreationState(
    val name: String,
    val surname: String,
    val dialog: ProfileCreationDialog?
) : EmaState {

    companion object {
        val DEFAULT = ProfileCreationState(
            name = STRING_EMPTY,
            surname = STRING_EMPTY,
            dialog = null
        )
    }

    val canCreate
        get() = name.isNotBlank() && surname.isNotBlank()
}
