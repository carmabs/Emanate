package com.carmabs.emanate.presentation.login

import com.carmabs.emanate.core.constants.STRING_EMPTY
import com.carmabs.emanate.core.state.EmaState

data class LoginState(
    val userName: String,
    val userPassword: String,
    val isLoading: Boolean,
    val userNameError: Boolean,
    val passwordError: Boolean,
    val dialog: LoginDialog?
) : EmaState {

    companion object {
        val DEFAULT = LoginState(
            userName = STRING_EMPTY,
            userPassword = STRING_EMPTY,
            isLoading = false,
            userNameError = false,
            passwordError = false,
            dialog = null
        )
    }
}
