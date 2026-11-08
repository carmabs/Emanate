package com.carmabs.emanate.presentation.login

import com.carmabs.domain.error.LoginException
import com.carmabs.domain.model.LoginRequest
import com.carmabs.domain.model.Role
import com.carmabs.domain.model.User
import com.carmabs.domain.repository.LoginRepository
import com.carmabs.domain.usecase.LoginUseCase
import com.carmabs.emanate.core.model.EmaResult
import com.carmabs.emanate.test.assertSetup
import com.carmabs.emanate.test.record
import com.carmabs.emanate.test.resetTestConfiguration
import com.carmabs.emanate.test.useTestConfiguration
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

private val admin = User("Carmabs", "Ema Creator", Role.ADMIN)

/**
 * Accepts the user Admin with the password 1234
 */
private class FakeLoginRepository : LoginRepository {

    var logins = 0

    override suspend fun login(loginRequest: LoginRequest): EmaResult<User, LoginException> {
        logins++
        delay(1000)
        return if (loginRequest.name == "Admin" && loginRequest.password == "1234") {
            EmaResult.success(admin)
        } else {
            EmaResult.failure(LoginException())
        }
    }
}

/**
 * Test of a view model built with EmaViewModel, using the utilities of emanate-test
 */
class LoginViewModelTest {

    private val repository = FakeLoginRepository()

    @BeforeTest
    fun setUp() {
        useTestConfiguration()
    }

    @AfterTest
    fun tearDown() {
        resetTestConfiguration()
    }

    private fun loginViewModel() = LoginViewModel(LoginUseCase(repository), LoginState.DEFAULT)

    @Test
    fun `the login without user name nor password shows the errors`() = runTest {
        val viewModel = loginViewModel()
        val recorder = viewModel.record()

        viewModel.dispatch(LoginAction.Login)
        advanceUntilIdle()

        recorder.assertState { it.userNameError && it.passwordError && !it.isLoading }
        recorder.assertNoEvents()
    }

    @Test
    fun `the right credentials log in the user`() = runTest {
        val viewModel = loginViewModel()
        val recorder = viewModel.record()

        viewModel.dispatch(LoginAction.UserNameWritten("Admin"))
        viewModel.dispatch(LoginAction.PasswordWritten("1234"))
        viewModel.dispatch(LoginAction.Login)
        advanceUntilIdle()

        recorder.assertState { !it.isLoading && it.dialog == null }
        recorder.assertEvents(LoginEvent.Message(admin.name), LoginEvent.LoginSuccess(admin))
        viewModel.close()
    }

    @Test
    fun `the wrong credentials show the error`() = runTest {
        val viewModel = loginViewModel()
        val recorder = viewModel.record()

        viewModel.dispatch(LoginAction.UserNameWritten("Admin"))
        viewModel.dispatch(LoginAction.PasswordWritten("0000"))
        viewModel.dispatch(LoginAction.Login)
        advanceUntilIdle()
        recorder.assertState { it.dialog == LoginDialog.ErrorBadCredentials }

        viewModel.dispatch(LoginAction.Error.BadCredentialsAccepted)
        advanceUntilIdle()
        recorder.assertState { it.dialog == null }
    }

    @Test
    fun `a second login while the first one is running is ignored`() = runTest {
        val viewModel = loginViewModel()

        viewModel.dispatch(LoginAction.UserNameWritten("Admin"))
        viewModel.dispatch(LoginAction.PasswordWritten("1234"))
        viewModel.dispatch(LoginAction.Login)
        viewModel.dispatch(LoginAction.Login)
        advanceUntilIdle()

        assertEquals(1, repository.logins)
    }

    @Test
    fun `the view model listens to the broadcast actions it handles`() {
        loginViewModel().assertSetup()
    }
}
