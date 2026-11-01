package com.carmabs.emanate.presentation.login

import com.carmabs.domain.model.User
import com.carmabs.domain.usecase.LoginUseCase
import com.carmabs.emanate.core.constants.STRING_EMPTY
import com.carmabs.emanate.core.initializer.EmaInitializer
import com.carmabs.emanate.core.model.onFailure
import com.carmabs.emanate.core.model.onSuccess
import com.carmabs.emanate.presentation.base.BaseActionViewModel
import com.carmabs.emanate.presentation.home.HomeBroadcast

class LoginActionViewModel(private val loginUseCase: LoginUseCase, initialDataState: LoginState) :
    BaseActionViewModel<LoginState, LoginAction, LoginEvent>(initialDataState) {
    override fun onStateCreated(initializer: EmaInitializer?) = Unit
    override fun onAction(action: LoginAction) {
        when (action) {
            LoginAction.DeleteUser -> {
                onActionDeleteUser()
            }

            LoginAction.Login -> {
                onActionLogin()
            }

            is LoginAction.PasswordWritten -> {
                onActionPasswordWrite(action.password)
            }

            is LoginAction.UserNameWritten -> {
                onActionUserWrite(action.user)
            }

            LoginAction.Error.BadCredentialsAccepted -> onActionBadCredentialsAccepted()

            LoginAction.Error.BackPressed -> onActionErrorBackPressed()

            // Only dispatched by LoginViewModel
            is LoginRequest -> Unit
        }
    }

    private fun onActionBadCredentialsAccepted() {
        hideOverlap()
    }

    private fun onActionErrorBackPressed() {
        hideOverlap()
    }

    private fun showOverlap(overlap: LoginOverlap) {
        updateState {
            copy(overlap = overlap)
        }
    }

    private fun hideOverlap() {
        updateState {
            copy(overlap = null)
        }
    }

    private var pendingUser: User? = null

    override fun onBroadcastListenerSetup() {
        registerBroadcastListener<HomeBroadcast.UserAdded> { action ->
            pendingUser = action.user
        }
    }

    override fun onViewResumed() {
        super.onViewResumed()
        pendingUser?.also {
            publish(LoginEvent.LastUserAdded(it))
        }
        pendingUser = null
    }

    private fun doLogin() {
        sideEffect {
            updateState {
                copy(isLoading = true)
            }
            val userLogged =
                loginUseCase.invoke(LoginUseCase.Input(state.userName, state.userPassword))
            updateState {
                copy(isLoading = false)
            }
            userLogged.onSuccess { user ->
                publish(LoginEvent.Message(user.name))
                publish(LoginEvent.LoginSuccess(user))
            }.onFailure {
                showOverlap(LoginOverlap.ErrorBadCredentials)
            }
        }
    }

    private fun onActionLogin() {
        if (state.isLoading) {
            return
        }
        val userNameError = state.userName.isBlank()
        val passwordError = state.userPassword.isBlank()
        if (userNameError || passwordError) {
            updateState {
                copy(userNameError = userNameError, passwordError = passwordError)
            }
        } else {
            doLogin()
        }
    }

    private fun onActionDeleteUser() {
        updateState {
            copy(
                userName = STRING_EMPTY,
                userPassword = STRING_EMPTY,
                userNameError = false,
                passwordError = false
            )
        }
    }

    private fun onActionUserWrite(user: String) {
        // The text is kept in the state so it is restored if, for example, there is a device rotation
        // and the view is recreated
        updateState {
            copy(userName = user, userNameError = false)
        }
    }

    private fun onActionPasswordWrite(password: String) {
        updateState {
            copy(userPassword = password, passwordError = false)
        }
    }
}
