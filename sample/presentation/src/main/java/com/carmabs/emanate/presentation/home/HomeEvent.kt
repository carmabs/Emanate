package com.carmabs.emanate.presentation.home

import com.carmabs.domain.model.Friend
import com.carmabs.emanate.core.state.EmaEvent

sealed interface HomeEvent : EmaEvent {
    data object CreateClicked : HomeEvent

    data class FriendCreated(val friend: Friend) : HomeEvent

    data object CloseClicked : HomeEvent
}
