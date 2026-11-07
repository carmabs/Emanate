package com.carmabs.app.repository

import com.carmabs.domain.model.LoginRequest
import com.carmabs.domain.model.User

/**
 * The server that checks the credentials. It returns the user, or null when they are not valid.
 *
 * The sample has no backend, so [LocalLoginApi] stands in for it. In a real app this is the interface that a Retrofit
 * or Ktor client implements, and [ApiLoginRepository] doesn't change.
 */
interface LoginApi {
    suspend fun login(request: LoginRequest): User?
}
