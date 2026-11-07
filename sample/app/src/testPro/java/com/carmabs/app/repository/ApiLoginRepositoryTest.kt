package com.carmabs.app.repository

import com.carmabs.domain.model.LoginRequest
import com.carmabs.domain.model.Role
import com.carmabs.emanate.core.model.EmaResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.ZERO
import kotlinx.coroutines.test.runTest

class ApiLoginRepositoryTest {

    private val repository = ApiLoginRepository(LocalLoginApi(latency = ZERO))

    @Test
    fun `valid credentials return the user`() = runTest {
        val result = repository.login(LoginRequest("admin", "1234"))

        assertIs<EmaResult.Success<*, *>>(result)
        assertEquals(Role.ADMIN, (result.data as com.carmabs.domain.model.User).role)
    }

    @Test
    fun `wrong credentials fail`() = runTest {
        val result = repository.login(LoginRequest("Admin", "0000"))

        assertIs<EmaResult.Failure<*, *>>(result)
    }
}
