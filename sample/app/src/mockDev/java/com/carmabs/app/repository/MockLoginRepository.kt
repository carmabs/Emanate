package com.carmabs.app.repository

import com.carmabs.domain.exception.LoginException
import com.carmabs.domain.model.LoginRequest
import com.carmabs.domain.model.Role
import com.carmabs.domain.model.User
import com.carmabs.domain.repository.LoginRepository
import com.carmabs.emanate.core.model.EmaResult
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 *  *<p>
 * Copyright (c) 2020, Carmabs. All rights reserved.
 * </p>
 *
 * @author <a href=“mailto:apps.carmabs@gmail.com”>Carlos Mateo Benito</a>
 *
 * Created by: Carlos Mateo Benito on 19/1/19.
 */
class MockLoginRepository(private val latency: Duration = 2.seconds) : LoginRepository {
    override suspend fun login(loginRequest: LoginRequest): EmaResult<User, LoginException> {
        delay(latency)
        return when {
            (loginRequest.name.equals("Admin", true) && loginRequest.password == "1234") ->
                EmaResult.success(User("Carmabs", "Ema Creator", Role.ADMIN))

            (loginRequest.name.equals("Basic", true) && loginRequest.password == "1234") -> {
                EmaResult.success(User("Anonymous", "Testing", Role.BASIC))
            }

            else -> {
                EmaResult.failure(LoginException())
            }
        }
    }
}
