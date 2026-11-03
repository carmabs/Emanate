package com.carmabs.app.di

import com.carmabs.emanate.core.savestate.EmaStateStore
import com.carmabs.emanate.presentation.home.HomeState
import com.carmabs.emanate.presentation.home.HomeViewModel
import com.carmabs.emanate.presentation.login.LoginState
import com.carmabs.emanate.presentation.login.LoginViewModel
import com.carmabs.emanate.presentation.profile.creation.ProfileCreationState
import com.carmabs.emanate.presentation.profile.creation.ProfileCreationViewModel
import com.carmabs.emanate.presentation.profile.onboarding.ProfileOnBoardingState
import com.carmabs.emanate.presentation.profile.onboarding.ProfileOnBoardingViewModel
import com.carmabs.emanate.presentation.splash.SplashViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val uiModule = module {

    factory { SplashViewModel() }
    factoryOf(::LoginViewModel)
    factoryOf(::HomeViewModel)
    factoryOf(::ProfileOnBoardingViewModel)

    // The platform gives the store of its backup when it creates the screen
    factory { (stateStore: EmaStateStore) -> ProfileCreationViewModel(get(), stateStore) }

    factory { LoginState.DEFAULT }
    factory { HomeState.DEFAULT }
    factory { ProfileOnBoardingState.DEFAULT }
    factory { ProfileCreationState.DEFAULT }
}
