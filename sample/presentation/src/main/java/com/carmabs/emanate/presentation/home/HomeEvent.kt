package com.carmabs.emanate.presentation.home

import com.carmabs.domain.model.User
import com.carmabs.emanate.core.state.EmaEvent

sealed interface HomeEvent : EmaEvent {
    data object CreateClicked : HomeEvent

    data class UserCreated(val user: User) : HomeEvent

    data object CloseClicked : HomeEvent
}
