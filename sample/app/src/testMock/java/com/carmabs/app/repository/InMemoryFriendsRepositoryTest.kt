package com.carmabs.app.repository

import com.carmabs.domain.model.Role
import com.carmabs.domain.model.User
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class InMemoryFriendsRepositoryTest {

    @Test
    fun `starts with the initial friends, each one with an id`() = runTest {
        val repository = InMemoryFriendsRepository(listOf(User("Ana", "Ruiz"), User("Leo", "Gil", id = "leo")))

        val friends = repository.getFriends()

        assertEquals(listOf("Ana", "Leo"), friends.map { it.name })
        assertTrue(friends[0].id.isNotEmpty())
        assertEquals("leo", friends[1].id)
    }

    @Test
    fun `adds basic friends at the end and deletes them by id`() = runTest {
        val repository = InMemoryFriendsRepository(listOf(User("Ana", "Ruiz")))

        val added = repository.addFriend("Marta", "Soler")
        assertEquals(Role.BASIC, added.role)
        assertEquals(listOf("Ana", "Marta"), repository.getFriends().map { it.name })

        repository.deleteFriend(added.id)
        assertEquals(listOf("Ana"), repository.getFriends().map { it.name })
    }
}
