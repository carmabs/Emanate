package com.carmabs.domain.usecase

import com.carmabs.domain.exception.LoginException
import com.carmabs.domain.model.LoginRequest
import com.carmabs.domain.model.User
import com.carmabs.domain.repository.LoginRepository
import com.carmabs.emanate.core.model.EmaResult
import com.carmabs.emanate.core.usecase.EmaUseCase

/**
 * Login
 *
 *
 * @author <a href=“mailto:apps.carmabs@gmail.com”>Carlos Mateo</a>
 */

class LoginUseCase(private val loginRepository: LoginRepository) :
    EmaUseCase<LoginUseCase.Input, EmaResult<User, LoginException>>() {

    override suspend fun useCaseFunction(input: Input): EmaResult<User, LoginException> =
        loginRepository.login(LoginRequest(input.username, input.password))

    data class Input(val username: String, val password: String)
}
