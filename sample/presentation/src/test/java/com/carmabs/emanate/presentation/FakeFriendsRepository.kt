package com.carmabs.emanate.presentation

import com.carmabs.domain.model.User
import com.carmabs.domain.repository.FriendsRepository

/**
 * Keeps the friends in memory, giving them consecutive ids
 */
class FakeFriendsRepository(friends: List<User> = emptyList()) : FriendsRepository {

    private val stored = friends.toMutableList()

    override suspend fun getFriends(): List<User> = stored.toList()

    override suspend fun addFriend(name: String, surname: String): User =
        User(name = name, surname = surname, id = (stored.size + 1).toString()).also { stored.add(it) }

    override suspend fun deleteFriend(id: String) {
        stored.removeAll { it.id == id }
    }
}
