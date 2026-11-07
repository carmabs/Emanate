package com.carmabs.emanate.presentation.profile.creation

import com.carmabs.domain.model.Friend
import com.carmabs.emanate.core.action.EmaAction

/**
 * Actions broadcast by the profile creation view models. Their type is the id of the broadcast.
 *
 * <p>
 * Copyright (c) 2026, Carmabs. All rights reserved.
 * </p>
 *
 * @author <a href=“mailto:apps.carmabs@gmail.com”>Carlos Mateo</a>
 */
sealed interface ProfileCreationBroadcast : EmaAction {

    /**
     * A friend has been created
     */
    data class FriendCreated(val friend: Friend) : ProfileCreationBroadcast
}
