package com.carmabs.emanate.presentation.profile.creation

import com.carmabs.domain.usecase.AddFriendUseCase
import com.carmabs.emanate.presentation.FakeFriendsRepository
import com.carmabs.emanate.core.initializer.EmaInitializer
import com.carmabs.emanate.test.record
import com.carmabs.emanate.test.resetTestConfiguration
import com.carmabs.emanate.test.restoreStateOf
import com.carmabs.emanate.test.testStateStore
import com.carmabs.emanate.test.useTestConfiguration
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

class ProfileCreationViewModelTest {

    @BeforeTest
    fun setUp() {
        useTestConfiguration()
    }

    @AfterTest
    fun tearDown() {
        resetTestConfiguration()
    }

    @Test
    fun `the form is restored after the death of the process`() = runTest {
        val store = testStateStore()
        val viewModel = ProfileCreationViewModel(AddFriendUseCase(FakeFriendsRepository()), ProfileCreationState.DEFAULT, store)
        viewModel.onCreated()
        viewModel.dispatch(ProfileCreationAction.UserNameWritten("Ana"))
        viewModel.dispatch(ProfileCreationAction.UserSurnameWritten("García"))
        viewModel.dispatch(ProfileCreationAction.OnBack)
        advanceUntilIdle()
        viewModel.close()

        // The system kills the process: the screen is created again with the saved values
        val restoredViewModel = ProfileCreationViewModel(AddFriendUseCase(FakeFriendsRepository()), ProfileCreationState.DEFAULT, store)
        val recorder = restoredViewModel.record()
        restoredViewModel.onCreated()
        advanceUntilIdle()

        recorder.assertActions(
            EmaInitializer.EMPTY,
            restoreStateOf("form" to """{"name":"Ana","surname":"García"}""")
        )
        // The dialog isn't saved, so it is closed
        recorder.assertState(ProfileCreationState("Ana", "García"))
        restoredViewModel.close()
    }

    @Test
    fun `the back press asks for confirmation only when there is no dialog`() = runTest {
        val viewModel = ProfileCreationViewModel(AddFriendUseCase(FakeFriendsRepository()), ProfileCreationState.DEFAULT, testStateStore())
        val recorder = viewModel.record()
        viewModel.onCreated()

        viewModel.dispatch(ProfileCreationAction.OnBack)
        advanceUntilIdle()
        recorder.assertState(ProfileCreationState.DEFAULT.copy(dialog = ProfileCreationDialog.BackConfirmation))

        // A second back press, with the dialog shown, changes nothing
        viewModel.dispatch(ProfileCreationAction.OnBack)
        advanceUntilIdle()
        recorder.assertState(ProfileCreationState.DEFAULT.copy(dialog = ProfileCreationDialog.BackConfirmation))
        viewModel.close()
    }

    @Test
    fun `a friend that can't be stored keeps the form and shows the error`() = runTest {
        val repository = FakeFriendsRepository().apply { failAdd = true }
        val viewModel = ProfileCreationViewModel(AddFriendUseCase(repository), ProfileCreationState.DEFAULT, testStateStore())
        val recorder = viewModel.record()
        viewModel.onCreated()
        viewModel.dispatch(ProfileCreationAction.UserNameWritten("Ana"))
        viewModel.dispatch(ProfileCreationAction.CreateClicked)

        viewModel.dispatch(ProfileCreationAction.DialogConfirmClicked)
        advanceUntilIdle()

        // The screen is not closed: there is no event, and the user can read the error and try again
        recorder.assertState(ProfileCreationState("Ana", "", ProfileCreationDialog.CreateError))
        recorder.assertEvents()
        viewModel.dispatch(ProfileCreationAction.DialogErrorAccepted)
        advanceUntilIdle()
        recorder.assertState(ProfileCreationState("Ana", ""))
        viewModel.close()
    }
}
