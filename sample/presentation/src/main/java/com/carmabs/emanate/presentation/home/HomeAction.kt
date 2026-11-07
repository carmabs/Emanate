package com.carmabs.emanate.presentation.home

import com.carmabs.domain.model.Friend
import com.carmabs.emanate.core.action.EmaAction

/**
 * Created by Carlos Mateo Benito on 6/3/24.
 *
 * <p>
 * Copyright (c) 2024 by Carmabs. All rights reserved.
 * </p>
 *
 * @author <a href=“mailto:apps.carmabs@gmail.com”>Carlos Mateo Benito</a>
 */
sealed interface HomeAction : EmaAction {
    data object CreateClicked : HomeAction

    data class DeleteClicked(val friend: Friend) : HomeAction

    data object CloseClicked : HomeAction

    /**
     * Actions dispatched by the side effects of HomeViewModel to report what they did
     */
    sealed interface SideEffects : HomeAction {
        /**
         * The friends of the storage have been read
         */
        data class FriendsLoaded(val friends: List<Friend>) : SideEffects
    }
}
