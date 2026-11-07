package com.carmabs.app.repository

import com.carmabs.domain.model.Friend
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class InMemoryFriendsRepositoryTest {

    @Test
    fun `starts with the initial friends, each one with its id`() = runTest {
        val repository = InMemoryFriendsRepository(listOf(Friend("ana", "Ana", "Ruiz"), Friend("leo", "Leo", "Gil")))

        val friends = repository.getFriends()

        assertEquals(listOf("Ana", "Leo"), friends.map { it.name })
        assertEquals(listOf("ana", "leo"), friends.map { it.id })
    }

    @Test
    fun `adds friends at the end and deletes them by id`() = runTest {
        val repository = InMemoryFriendsRepository(listOf(Friend("ana", "Ana", "Ruiz")))

        val added = repository.addFriend("Marta", "Soler")
        assertTrue(added.id.isNotEmpty())
        assertEquals(listOf("Ana", "Marta"), repository.getFriends().map { it.name })

        repository.deleteFriend(added.id)
        assertEquals(listOf("Ana"), repository.getFriends().map { it.name })
    }
}
