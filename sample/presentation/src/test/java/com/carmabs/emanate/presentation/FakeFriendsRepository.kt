package com.carmabs.emanate.presentation

import com.carmabs.domain.error.FriendsException
import com.carmabs.domain.model.Friend
import com.carmabs.domain.repository.FriendsRepository
import com.carmabs.emanate.core.model.EmaResult

/**
 * Keeps the friends in memory, giving them consecutive ids. Each operation can be made to fail, like a storage that
 * can't be reached.
 */
class FakeFriendsRepository(friends: List<Friend> = emptyList()) : FriendsRepository {

    private val stored = friends.toMutableList()

    var failGet = false
    var failAdd = false
    var failDelete = false

    override suspend fun getFriends(): EmaResult<List<Friend>, FriendsException> =
        if (failGet) EmaResult.failure(FriendsException()) else EmaResult.success(stored.toList())

    override suspend fun addFriend(name: String, surname: String): EmaResult<Friend, FriendsException> =
        if (failAdd) {
            EmaResult.failure(FriendsException())
        } else {
            EmaResult.success(
                Friend(id = (stored.size + 1).toString(), name = name, surname = surname).also { stored.add(it) }
            )
        }

    override suspend fun deleteFriend(id: String): EmaResult<Unit, FriendsException> =
        if (failDelete) {
            EmaResult.failure(FriendsException())
        } else {
            stored.removeAll { it.id == id }
            EmaResult.success(Unit)
        }
}
