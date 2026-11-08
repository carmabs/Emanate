package com.carmabs.emanate.presentation.profile.creation

import com.carmabs.emanate.core.constants.STRING_EMPTY
import com.carmabs.emanate.core.state.EmaState

data class ProfileCreationState(
    val name: String,
    val surname: String,
    val dialog: ProfileCreationDialog? = null
) : EmaState {

    companion object {
        val DEFAULT = ProfileCreationState(
            STRING_EMPTY,
            STRING_EMPTY
        )
    }

    val canCreate
        get() = name.isNotBlank() && surname.isNotBlank()
}
