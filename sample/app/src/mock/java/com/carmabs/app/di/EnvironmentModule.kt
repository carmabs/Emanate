package com.carmabs.app.di

import com.carmabs.app.repository.InMemoryFriendsRepository
import com.carmabs.app.repository.MockLoginRepository
import com.carmabs.domain.model.Friend
import com.carmabs.domain.repository.FriendsRepository
import com.carmabs.domain.repository.LoginRepository
import com.carmabs.emanate.ui.environment.AppEnvironment
import kotlin.time.Duration.Companion.ZERO
import org.koin.dsl.module

/**
 * Mock: everything answers at once and nothing is saved, so the app always starts the same way with some friends.
 */
val environmentModule = module {
    single { AppEnvironment("mock") }

    single<LoginRepository> { MockLoginRepository(latency = ZERO) }

    single<FriendsRepository> {
        InMemoryFriendsRepository(
            listOf(
                Friend("mock-1", "Lucía", "Navarro"),
                Friend("mock-2", "Marcos", "Ferrer"),
                Friend("mock-3", "Elena", "Vidal")
            )
        )
    }
}
