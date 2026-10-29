package com.carmabs.emanate.presentation.profile.creation

import com.carmabs.domain.model.Role
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
        val viewModel = ProfileCreationViewModel(ProfileCreationState.DEFAULT, store)
        viewModel.onCreated(ProfileCreationInitializer.Admin)
        viewModel.dispatch(ProfileCreationAction.UserNameWritten("Ana"))
        viewModel.dispatch(ProfileCreationAction.UserSurnameWritten("García"))
        viewModel.dispatch(ProfileCreationAction.OnBack)
        advanceUntilIdle()
        viewModel.close()

        // The system kills the process: the screen is created again with the same initializer and the saved values
        val restoredViewModel = ProfileCreationViewModel(ProfileCreationState.DEFAULT, store)
        val recorder = restoredViewModel.record()
        restoredViewModel.onCreated(ProfileCreationInitializer.Admin)
        advanceUntilIdle()

        recorder.assertActions(
            ProfileCreationInitializer.Admin,
            restoreStateOf("form" to """{"name":"Ana","surname":"García"}""")
        )
        // The dialog isn't saved, so it is closed
        recorder.assertState(ProfileCreationState(Role.ADMIN, "Ana", "García"))
        restoredViewModel.close()
    }
}
