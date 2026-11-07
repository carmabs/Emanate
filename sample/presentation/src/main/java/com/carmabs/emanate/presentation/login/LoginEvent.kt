package com.carmabs.emanate.presentation.login

import com.carmabs.domain.model.User
import com.carmabs.emanate.core.state.EmaEvent

sealed interface LoginEvent : EmaEvent {

    data class Message(val userName: String) : LoginEvent

    data class LoginSuccess(val user: User) : LoginEvent
}
