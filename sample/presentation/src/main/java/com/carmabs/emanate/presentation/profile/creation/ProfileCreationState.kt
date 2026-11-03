package com.carmabs.emanate.presentation.profile.creation

import com.carmabs.domain.model.Role
import com.carmabs.emanate.core.constants.STRING_EMPTY
import com.carmabs.emanate.core.state.EmaState

data class ProfileCreationState(
    val role: Role,
    val name: String,
    val surname: String,
    val dialog: ProfileCreationDialog? = null
) : EmaState {

    companion object {
        val DEFAULT = ProfileCreationState(
            Role.BASIC,
            STRING_EMPTY,
            STRING_EMPTY
        )
    }

    val canCreate
        get() = name.isNotBlank()
}
