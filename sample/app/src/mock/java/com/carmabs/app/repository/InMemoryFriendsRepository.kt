package com.carmabs.app.repository

import com.carmabs.domain.model.Role
import com.carmabs.domain.model.User
import com.carmabs.domain.repository.FriendsRepository
import java.util.UUID
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Keeps the friends in memory, starting with [initial]. Nothing is saved, so the app always starts the same way: it is
 * the repository of the mock environment, for demos and for working without data.
 */
class InMemoryFriendsRepository(initial: List<User> = emptyList()) : FriendsRepository {

    // The map keeps the order of insertion, so the friends are listed in the order they were created
    private val friends: MutableMap<String, User> = linkedMapOf<String, User>().apply {
        initial.forEach { friend ->
            val id = friend.id.ifEmpty { UUID.randomUUID().toString() }
            put(id, friend.copy(id = id))
        }
    }

    private val mutex = Mutex()

    override suspend fun getFriends(): List<User> = mutex.withLock { friends.values.toList() }

    override suspend fun addFriend(name: String, surname: String): User = mutex.withLock {
        val id = UUID.randomUUID().toString()
        User(name = name, surname = surname, role = Role.BASIC, id = id).also { friends[id] = it }
    }

    override suspend fun deleteFriend(id: String) {
        mutex.withLock { friends.remove(id) }
    }
}
