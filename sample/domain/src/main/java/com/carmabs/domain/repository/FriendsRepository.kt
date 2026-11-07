package com.carmabs.domain.repository

import com.carmabs.domain.model.User

/**
 * Friends created by the admin. They are stored on the device, so they are still there the next time the app opens.
 */
interface FriendsRepository {

    suspend fun getFriends(): List<User>

    /**
     * Stores a new basic user and returns it with the id given to it.
     */
    suspend fun addFriend(name: String, surname: String): User

    suspend fun deleteFriend(id: String)
}
