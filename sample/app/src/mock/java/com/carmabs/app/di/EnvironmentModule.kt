package com.carmabs.app.di

import com.carmabs.app.repository.InMemoryFriendsRepository
import com.carmabs.app.repository.MockLoginRepository
import com.carmabs.domain.model.User
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
                User("Lucía", "Navarro"),
                User("Marcos", "Ferrer"),
                User("Elena", "Vidal")
            )
        )
    }
}
