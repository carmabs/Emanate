package com.carmabs.app.di

import android.content.Context
import com.carmabs.data.repository.MapFriendsRepository
import com.carmabs.data.repository.MockLoginRepository
import com.carmabs.domain.repository.FriendsRepository
import com.carmabs.domain.repository.LoginRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 *  *<p>
 * Copyright (c) 2020, Carmabs. All rights reserved.
 * </p>
 *
 * @author <a href=“mailto:apps.carmabs@gmail.com”>Carlos Mateo Benito</a>
 *
 * Created by: Carlos Mateo Benito on 20/1/19.
 */

val dataModule = module {

    single<LoginRepository> {
        MockLoginRepository()
    }

    single<FriendsRepository> {
        MapFriendsRepository(
            androidContext().getSharedPreferences(MapFriendsRepository.PREFERENCES_NAME, Context.MODE_PRIVATE)
        )
    }
}
