package com.carmabs.app.di

import android.content.Context
import com.carmabs.app.repository.ApiLoginRepository
import com.carmabs.app.repository.LocalLoginApi
import com.carmabs.data.repository.MapFriendsRepository
import com.carmabs.domain.repository.FriendsRepository
import com.carmabs.domain.repository.LoginRepository
import com.carmabs.emanate.ui.environment.AppEnvironment
import com.carmabs.emanate.ui.environment.EnvironmentTone
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Pro: the login goes through an API, which here is local because the sample has no backend, and the friends are saved.
 */
val environmentModule = module {
    single { AppEnvironment("pro", tone = EnvironmentTone.Invisible) }

    single<LoginRepository> { ApiLoginRepository(LocalLoginApi()) }

    single<FriendsRepository> {
        MapFriendsRepository(
            androidContext().getSharedPreferences(MapFriendsRepository.PREFERENCES_NAME, Context.MODE_PRIVATE)
        )
    }
}
