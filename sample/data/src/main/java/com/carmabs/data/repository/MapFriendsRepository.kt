package com.carmabs.data.repository

import android.content.SharedPreferences
import com.carmabs.domain.model.Role
import com.carmabs.domain.model.User
import com.carmabs.domain.repository.FriendsRepository
import com.carmabs.emanate.sample.androidutils.getJson
import com.carmabs.emanate.sample.androidutils.putJson
import java.util.UUID
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer

/**
 * Stores the friends in a map, by id, that is saved in the [SharedPreferences] as JSON every time it changes.
 *
 * The right place for this data is a local database like Room. A map in the preferences is used only to keep the
 * sample simple: the repository is the only piece that knows how the friends are stored, so changing it to Room doesn't
 * affect the use cases nor the view models.
 */
class MapFriendsRepository(private val preferences: SharedPreferences) : FriendsRepository {

    private val serializer = MapSerializer(String.serializer(), User.serializer())

    // The map keeps the order of insertion, so the friends are listed in the order they were created
    private val friends: MutableMap<String, User> by lazy { read() }

    private val mutex = Mutex()

    override suspend fun getFriends(): List<User> = mutex.withLock { friends.values.toList() }

    override suspend fun addFriend(name: String, surname: String): User = mutex.withLock {
        val id = UUID.randomUUID().toString()
        val friend = User(name = name, surname = surname, role = Role.BASIC, id = id)
        friends[id] = friend
        write()
        friend
    }

    override suspend fun deleteFriend(id: String) {
        mutex.withLock {
            friends.remove(id)
            write()
        }
    }

    private fun read(): MutableMap<String, User> =
        preferences.getJson(KEY_FRIENDS, serializer, emptyMap()).toMutableMap()

    private fun write() = preferences.putJson(KEY_FRIENDS, serializer, friends)

    companion object {
        const val PREFERENCES_NAME = "friends"
        private const val KEY_FRIENDS = "friends"
    }
}
