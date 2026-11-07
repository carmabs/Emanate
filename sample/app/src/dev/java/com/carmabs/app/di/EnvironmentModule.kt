package com.carmabs.app.di

import android.content.Context
import com.carmabs.app.repository.DelayedFriendsRepository
import com.carmabs.app.repository.MockLoginRepository
import com.carmabs.data.repository.MapFriendsRepository
import com.carmabs.domain.repository.FriendsRepository
import com.carmabs.domain.repository.LoginRepository
import com.carmabs.emanate.ui.environment.AppEnvironment
import com.carmabs.emanate.ui.environment.EnvironmentTone
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Dev: the friends are saved, and the login and the friends are slow, to see the loading states and the animations.
 */
val environmentModule = module {
    single { AppEnvironment("dev", tone = EnvironmentTone.Secondary) }

    single<LoginRepository> { MockLoginRepository(latency = 2.seconds) }

    single<FriendsRepository> {
        DelayedFriendsRepository(
            delegate = MapFriendsRepository(
                androidContext().getSharedPreferences(MapFriendsRepository.PREFERENCES_NAME, Context.MODE_PRIVATE)
            ),
            latency = 800.milliseconds
        )
    }
}
