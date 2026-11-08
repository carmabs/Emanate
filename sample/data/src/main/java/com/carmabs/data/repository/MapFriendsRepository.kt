package com.carmabs.data.repository

import android.content.SharedPreferences
import com.carmabs.domain.exception.FriendsException
import com.carmabs.domain.model.Friend
import com.carmabs.domain.repository.FriendsRepository
import com.carmabs.emanate.core.model.EmaResult
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

    private val serializer = MapSerializer(String.serializer(), Friend.serializer())

    // The map keeps the order of insertion, so the friends are listed in the order they were created
    private val friends: MutableMap<String, Friend> by lazy { read() }

    private val mutex = Mutex()

    override suspend fun getFriends(): EmaResult<List<Friend>, FriendsException> =
        storage { friends.values.toList() }

    override suspend fun addFriend(name: String, surname: String): EmaResult<Friend, FriendsException> = storage {
        val id = UUID.randomUUID().toString()
        val friend = Friend(id = id, name = name, surname = surname)
        write(friends + (id to friend))
        friends[id] = friend
        friend
    }

    override suspend fun deleteFriend(id: String): EmaResult<Unit, FriendsException> = storage {
        write(friends - id)
        friends.remove(id)
        Unit
    }

    // The map in memory changes only after the preferences have been written, so a failure leaves both as they were
    private suspend fun <T> storage(block: () -> T): EmaResult<T, FriendsException> = mutex.withLock {
        try {
            EmaResult.success(block())
        } catch (exception: Exception) {
            EmaResult.failure(FriendsException())
        }
    }

    private fun read(): MutableMap<String, Friend> =
        preferences.getJson(KEY_FRIENDS, serializer, emptyMap()).toMutableMap()

    private fun write(friends: Map<String, Friend>) = preferences.putJson(KEY_FRIENDS, serializer, friends)

    companion object {
        const val PREFERENCES_NAME = "friends"
        private const val KEY_FRIENDS = "friends"
    }
}
