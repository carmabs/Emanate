package com.carmabs.domain.repository

import com.carmabs.domain.exception.FriendsException
import com.carmabs.domain.model.Friend
import com.carmabs.emanate.core.model.EmaResult

/**
 * Friends created by the admin. They are stored on the device, so they are still there the next time the app opens.
 */
interface FriendsRepository {

    suspend fun getFriends(): EmaResult<List<Friend>, FriendsException>

    /**
     * Stores a new friend and returns it with the id given to it.
     */
    suspend fun addFriend(name: String, surname: String): EmaResult<Friend, FriendsException>

    suspend fun deleteFriend(id: String): EmaResult<Unit, FriendsException>
}
