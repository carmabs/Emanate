package com.carmabs.app.repository

import com.carmabs.domain.error.LoginException
import com.carmabs.domain.model.LoginRequest
import com.carmabs.domain.model.User
import com.carmabs.domain.repository.LoginRepository
import com.carmabs.emanate.core.model.EmaResult

/**
 * Logs in through a [LoginApi]: the repository of the pro environment.
 */
class ApiLoginRepository(private val api: LoginApi) : LoginRepository {

    override suspend fun login(loginRequest: LoginRequest): EmaResult<User, LoginException> =
        api.login(loginRequest)
            ?.let { EmaResult.success(it) }
            ?: EmaResult.failure(LoginException())
}
