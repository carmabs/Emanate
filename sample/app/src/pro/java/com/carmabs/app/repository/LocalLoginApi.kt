package com.carmabs.app.repository

import com.carmabs.domain.model.LoginRequest
import com.carmabs.domain.model.Role
import com.carmabs.domain.model.User
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

/**
 * Checks the credentials against the two users of the sample, after a short wait like a real request.
 */
class LocalLoginApi(private val latency: Duration = 300.milliseconds) : LoginApi {

    override suspend fun login(request: LoginRequest): User? {
        delay(latency)
        return when {
            request.name.equals("Admin", true) && request.password == "1234" ->
                User("Carmabs", "Ema Creator", Role.ADMIN)

            request.name.equals("Basic", true) && request.password == "1234" ->
                User("Anonymous", "Testing", Role.BASIC)

            else -> null
        }
    }
}
