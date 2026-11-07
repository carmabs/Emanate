package com.carmabs.app.repository

import com.carmabs.domain.model.Friend
import com.carmabs.domain.repository.FriendsRepository
import java.util.UUID
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Keeps the friends in memory, starting with [initial]. Nothing is saved, so the app always starts the same way: it is
 * the repository of the mock environment, for demos and for working without data.
 */
class InMemoryFriendsRepository(initial: List<Friend> = emptyList()) : FriendsRepository {

    // The map keeps the order of insertion, so the friends are listed in the order they were created
    private val friends: MutableMap<String, Friend> = linkedMapOf<String, Friend>().apply {
        initial.forEach { friend -> put(friend.id, friend) }
    }

    private val mutex = Mutex()

    override suspend fun getFriends(): List<Friend> = mutex.withLock { friends.values.toList() }

    override suspend fun addFriend(name: String, surname: String): Friend = mutex.withLock {
        val id = UUID.randomUUID().toString()
        Friend(id = id, name = name, surname = surname).also { friends[id] = it }
    }

    override suspend fun deleteFriend(id: String) {
        mutex.withLock { friends.remove(id) }
    }
}
