package com.carmabs.emanate.presentation.login

import com.carmabs.domain.exception.LoginException
import com.carmabs.domain.model.LoginRequest
import com.carmabs.domain.model.Role
import com.carmabs.domain.model.User
import com.carmabs.domain.repository.Repository
import com.carmabs.domain.usecase.GetUserFriendsUseCase
import com.carmabs.domain.usecase.LoginUseCase
import com.carmabs.emanate.core.model.EmaResult
import com.carmabs.emanate.core.savestate.EmaStateStore
import com.carmabs.emanate.presentation.home.HomeInitializer
import com.carmabs.emanate.presentation.home.HomeState
import com.carmabs.emanate.presentation.home.HomeViewModel
import com.carmabs.emanate.presentation.profile.creation.ProfileCreationAction
import com.carmabs.emanate.presentation.profile.creation.ProfileCreationInitializer
import com.carmabs.emanate.presentation.profile.creation.ProfileCreationState
import com.carmabs.emanate.presentation.profile.creation.ProfileCreationViewModel
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
private class FakeRepository : Repository {

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

    override suspend fun getFriendsList(user: User): List<User> = emptyList()
}

/**
 * Test of a view model built with EmaViewModel, using the utilities of emanate-test
 */
class LoginViewModelTest {

    private val repository = FakeRepository()

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

        recorder.assertState { !it.isLoading && it.overlap == null }
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
        recorder.assertState { it.overlap == LoginOverlap.ErrorBadCredentials }

        viewModel.dispatch(LoginAction.Error.BadCredentialsAccepted)
        advanceUntilIdle()
        recorder.assertState { it.overlap == null }
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
    fun `the users created in the profile screen are notified`() = runTest {
        val viewModel = loginViewModel()
        val recorder = viewModel.record()
        val homeViewModel = HomeViewModel(GetUserFriendsUseCase(repository), HomeState.DEFAULT)
        homeViewModel.onCreated(HomeInitializer.HomeUser(admin))
        val profileCreationViewModel = ProfileCreationViewModel(ProfileCreationState.DEFAULT, EmaStateStore.None)
        profileCreationViewModel.onCreated(ProfileCreationInitializer.UserBasic)

        profileCreationViewModel.dispatch(ProfileCreationAction.UserNameWritten("Ana"))
        profileCreationViewModel.dispatch(ProfileCreationAction.CreateClicked)
        profileCreationViewModel.dispatch(ProfileCreationAction.DialogConfirmClicked)
        advanceUntilIdle()

        val friend = User("Ana", "", Role.BASIC)
        // The profile screen broadcasts the user to the home screen, which broadcasts it to the login screen
        assertEquals(listOf(friend), homeViewModel.stateFlow.value.userList)
        recorder.assertEvents(LoginEvent.LastUserAdded(friend))
        profileCreationViewModel.close()
        homeViewModel.close()
        viewModel.close()
    }

    @Test
    fun `the view models listen to the broadcast actions they handle`() {
        loginViewModel().assertSetup()
        HomeViewModel(GetUserFriendsUseCase(repository), HomeState.DEFAULT).assertSetup()
        ProfileCreationViewModel(ProfileCreationState.DEFAULT, EmaStateStore.None).assertSetup()
    }
}
