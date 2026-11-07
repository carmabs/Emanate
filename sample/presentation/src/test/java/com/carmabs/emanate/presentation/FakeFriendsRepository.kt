package com.carmabs.emanate.presentation

import com.carmabs.domain.model.Friend
import com.carmabs.domain.repository.FriendsRepository

/**
 * Keeps the friends in memory, giving them consecutive ids
 */
class FakeFriendsRepository(friends: List<Friend> = emptyList()) : FriendsRepository {

    private val stored = friends.toMutableList()

    override suspend fun getFriends(): List<Friend> = stored.toList()

    override suspend fun addFriend(name: String, surname: String): Friend =
        Friend(id = (stored.size + 1).toString(), name = name, surname = surname).also { stored.add(it) }

    override suspend fun deleteFriend(id: String) {
        stored.removeAll { it.id == id }
    }
}
