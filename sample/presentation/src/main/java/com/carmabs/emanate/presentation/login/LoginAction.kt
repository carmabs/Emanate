package com.carmabs.emanate.presentation.login

import com.carmabs.domain.model.User
import com.carmabs.emanate.core.action.EmaAction

/**
 * Created by Carlos Mateo Benito on 7/3/24.
 *
 * <p>
 * Copyright (c) 2024 by Carmabs. All rights reserved.
 * </p>
 *
 * @author <a href=“mailto:apps.carmabs@gmail.com”>Carlos Mateo Benito</a>
 */
sealed interface LoginAction : EmaAction {

    data object Login : LoginAction

    data object DeleteUser : LoginAction

    data class UserNameWritten(val user: String) : LoginAction

    data class PasswordWritten(val password: String) : LoginAction

    sealed interface Error : LoginAction {
        data object BadCredentialsAccepted : Error
        data object BackPressed : Error
    }

    /**
     * Actions dispatched by the side effects of LoginViewModel, the steps of the login request
     */
    sealed interface SideEffects : LoginAction {
        data object LoginStarted : SideEffects
        data class LoginSucceeded(val user: User) : SideEffects
        data object LoginFailed : SideEffects
    }
}
