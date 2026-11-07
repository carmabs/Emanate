package com.carmabs.emanate.presentation.login

import com.carmabs.domain.usecase.LoginUseCase
import com.carmabs.emanate.core.constants.STRING_EMPTY
import com.carmabs.emanate.core.model.onFailure
import com.carmabs.emanate.core.model.onSuccess
import com.carmabs.emanate.core.source.reducer.reducerOf
import com.carmabs.emanate.core.source.sideeffect.SideEffectConcurrency
import com.carmabs.emanate.core.source.sideeffect.sideEffectOf
import com.carmabs.emanate.presentation.base.BaseViewModel

class LoginViewModel(private val loginUseCase: LoginUseCase, initialDataState: LoginState) :
    BaseViewModel<LoginState, LoginAction, LoginEvent>(initialDataState) {

    private val reducerAction = reducerOf(LoginAction::class) { action ->
        when (action) {
            is LoginAction.UserNameWritten -> state.copy(userName = action.user, userNameError = false)

            is LoginAction.PasswordWritten -> state.copy(userPassword = action.password, passwordError = false)

            LoginAction.DeleteUser -> state.copy(
                userName = STRING_EMPTY,
                userPassword = STRING_EMPTY,
                userNameError = false,
                passwordError = false
            )

            LoginAction.Login -> if (state.isLoading) {
                state
            } else {
                state.copy(
                    userNameError = state.userName.isBlank(),
                    passwordError = state.userPassword.isBlank()
                )
            }

            LoginAction.Error.BadCredentialsAccepted,
            LoginAction.Error.BackPressed -> state.copy(dialog = null)

            // They are reduced by reducerSideEffects
            is LoginAction.SideEffects -> state
        }
    }

    private val reducerSideEffects = reducerOf(LoginAction.SideEffects::class) { action ->
        when (action) {
            LoginAction.SideEffects.LoginStarted -> state.copy(isLoading = true)

            is LoginAction.SideEffects.LoginSucceeded -> state.copy(isLoading = false)

            LoginAction.SideEffects.LoginFailed -> state.copy(isLoading = false, dialog = LoginDialog.ErrorBadCredentials)
        }
    }

    // Leading: a second tap while the request is running is ignored
    private val sideEffectLogin = sideEffectOf(LoginAction.Login::class, SideEffectConcurrency.Leading) {
        if (state.isLoading || state.userNameError || state.passwordError) return@sideEffectOf
        dispatch(LoginAction.SideEffects.LoginStarted)
        loginUseCase(LoginUseCase.Input(state.userName, state.userPassword))
            .onSuccess { user -> dispatch(LoginAction.SideEffects.LoginSucceeded(user)) }
            .onFailure { dispatch(LoginAction.SideEffects.LoginFailed) }
    }

    private val sideEffectPublishLoginSuccess = sideEffectOf(LoginAction.SideEffects.LoginSucceeded::class) { action ->
        publish(LoginEvent.Message(action.user.name))
        publish(LoginEvent.LoginSuccess(action.user))
    }

    override fun setup() = source {
        reducers {
            add(reducerAction, reducerSideEffects)
        }

        sideEffects {
            add(sideEffectLogin, sideEffectPublishLoginSuccess)
        }
    }
}
