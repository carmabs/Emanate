package com.carmabs.app.repository

import com.carmabs.domain.model.User
import com.carmabs.domain.repository.FriendsRepository
import kotlin.time.Duration
import kotlinx.coroutines.delay

/**
 * Makes every call of [delegate] take [latency] more, as a slow network would. It is used in the dev environment to see
 * the loading states and the animations.
 */
class DelayedFriendsRepository(
    private val delegate: FriendsRepository,
    private val latency: Duration
) : FriendsRepository {

    override suspend fun getFriends(): List<User> {
        delay(latency)
        return delegate.getFriends()
    }

    override suspend fun addFriend(name: String, surname: String): User {
        delay(latency)
        return delegate.addFriend(name, surname)
    }

    override suspend fun deleteFriend(id: String) {
        delay(latency)
        delegate.deleteFriend(id)
    }
}
