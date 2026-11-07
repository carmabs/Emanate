package com.carmabs.domain.repository

import com.carmabs.domain.model.Friend

/**
 * Friends created by the admin. They are stored on the device, so they are still there the next time the app opens.
 */
interface FriendsRepository {

    suspend fun getFriends(): List<Friend>

    /**
     * Stores a new friend and returns it with the id given to it.
     */
    suspend fun addFriend(name: String, surname: String): Friend

    suspend fun deleteFriend(id: String)
}
