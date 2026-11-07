package com.carmabs.app.di

import com.carmabs.domain.usecase.AddFriendUseCase
import com.carmabs.domain.usecase.DeleteFriendUseCase
import com.carmabs.domain.usecase.GetFriendsUseCase
import com.carmabs.domain.usecase.LoginUseCase
import org.koin.dsl.module

val useCaseModule = module {
    factory { LoginUseCase(get()) }
    factory { GetFriendsUseCase(get()) }
    factory { AddFriendUseCase(get()) }
    factory { DeleteFriendUseCase(get()) }
}
