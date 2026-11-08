package com.carmabs.emanate.presentation.home

import com.carmabs.domain.model.Friend
import com.carmabs.emanate.core.state.EmaEvent

sealed interface HomeEvent : EmaEvent {
    data object CreateClicked : HomeEvent

    data class FriendCreated(val friend: Friend) : HomeEvent

    data object CloseClicked : HomeEvent

    /**
     * The friends couldn't be read from the storage
     */
    data object LoadFailed : HomeEvent

    /**
     * The friend couldn't be deleted from the storage, so it is back in the list
     */
    data class DeleteFailed(val friend: Friend) : HomeEvent
}
