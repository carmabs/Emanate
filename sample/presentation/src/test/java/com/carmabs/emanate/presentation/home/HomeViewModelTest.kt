package com.carmabs.emanate.presentation.home

import com.carmabs.domain.model.Friend
import com.carmabs.domain.model.Role
import com.carmabs.domain.model.User
import com.carmabs.domain.usecase.AddFriendUseCase
import com.carmabs.domain.usecase.DeleteFriendUseCase
import com.carmabs.domain.usecase.GetFriendsUseCase
import com.carmabs.emanate.core.savestate.EmaStateStore
import com.carmabs.emanate.presentation.FakeFriendsRepository
import com.carmabs.emanate.presentation.profile.creation.ProfileCreationAction
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
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

private val admin = User("Carmabs", "Ema Creator", Role.ADMIN)
private val ana = Friend("1", "Ana", "López")
private val luis = Friend("2", "Luis", "Pérez")

class HomeViewModelTest {

    private val repository = FakeFriendsRepository(listOf(ana, luis))

    @BeforeTest
    fun setUp() {
        useTestConfiguration()
    }

    @AfterTest
    fun tearDown() {
        resetTestConfiguration()
    }

    private fun homeViewModel() =
        HomeViewModel(GetFriendsUseCase(repository), DeleteFriendUseCase(repository), HomeState.DEFAULT)

    @Test
    fun `the stored friends are listed when the screen is created`() = runTest {
        val viewModel = homeViewModel()
        val recorder = viewModel.record()

        viewModel.onCreated(HomeInitializer.HomeUser(admin))
        advanceUntilIdle()

        recorder.assertState { it.friends == listOf(ana, luis) && it.friendsLoaded && !it.showEmptyList }
        viewModel.close()
    }

    @Test
    fun `an empty list is shown when nobody has been created`() = runTest {
        val viewModel = HomeViewModel(
            GetFriendsUseCase(FakeFriendsRepository()),
            DeleteFriendUseCase(repository),
            HomeState.DEFAULT
        )
        val recorder = viewModel.record()

        viewModel.onCreated(HomeInitializer.HomeUser(User("User", "Testing", Role.BASIC)))
        advanceUntilIdle()

        recorder.assertState { it.showEmptyList && !it.showCreateButton && !it.showDeleteButtons }
        viewModel.close()
    }

    @Test
    fun `the admin can create and delete users, the basic user can't`() = runTest {
        val adminViewModel = homeViewModel()
        val adminRecorder = adminViewModel.record()
        adminViewModel.onCreated(HomeInitializer.HomeUser(admin))
        advanceUntilIdle()
        adminRecorder.assertState { it.showCreateButton && it.showDeleteButtons }
        adminViewModel.close()

        val basicViewModel = homeViewModel()
        val basicRecorder = basicViewModel.record()
        basicViewModel.onCreated(HomeInitializer.HomeUser(User("User", "Testing", Role.BASIC)))
        advanceUntilIdle()
        basicRecorder.assertState { !it.showCreateButton && !it.showDeleteButtons }
        basicViewModel.close()
    }

    @Test
    fun `a deleted friend disappears from the list and from the storage`() = runTest {
        val viewModel = homeViewModel()
        val recorder = viewModel.record()
        viewModel.onCreated(HomeInitializer.HomeUser(admin))
        advanceUntilIdle()

        viewModel.dispatch(HomeAction.DeleteClicked(ana))
        advanceUntilIdle()

        recorder.assertState { it.friends == listOf(luis) }
        assertEquals(listOf(luis), repository.getFriends().getOrThrow())
        viewModel.close()
    }

    @Test
    fun `a friend that can't be deleted comes back to the list and the failure is published`() = runTest {
        val viewModel = homeViewModel()
        val recorder = viewModel.record()
        viewModel.onCreated(HomeInitializer.HomeUser(admin))
        advanceUntilIdle()
        repository.failDelete = true

        viewModel.dispatch(HomeAction.DeleteClicked(ana))
        advanceUntilIdle()

        recorder.assertState { it.friends == listOf(ana, luis) }
        recorder.assertEvents(HomeEvent.DeleteFailed(ana))
        viewModel.close()
    }

    @Test
    fun `the failure is published when the friends can't be read`() = runTest {
        repository.failGet = true
        val viewModel = homeViewModel()
        val recorder = viewModel.record()

        viewModel.onCreated(HomeInitializer.HomeUser(admin))
        advanceUntilIdle()

        recorder.assertState { it.friends.isEmpty() && !it.friendsLoaded && !it.showEmptyList }
        recorder.assertEvents(HomeEvent.LoadFailed)
        viewModel.close()
    }

    @Test
    fun `the buttons of the corners publish their events`() = runTest {
        val viewModel = homeViewModel()
        val recorder = viewModel.record()

        viewModel.dispatch(HomeAction.CreateClicked)
        viewModel.dispatch(HomeAction.CloseClicked)
        advanceUntilIdle()

        recorder.assertEvents(HomeEvent.CreateClicked, HomeEvent.CloseClicked)
        viewModel.close()
    }

    @Test
    fun `the friend created in the profile screen is stored, listed and notified`() = runTest {
        val viewModel = homeViewModel()
        val recorder = viewModel.record()
        viewModel.onCreated(HomeInitializer.HomeUser(admin))
        val profileCreationViewModel = ProfileCreationViewModel(
            AddFriendUseCase(repository),
            ProfileCreationState.DEFAULT,
            EmaStateStore.None
        )
        profileCreationViewModel.onCreated()

        profileCreationViewModel.dispatch(ProfileCreationAction.UserNameWritten("Marta"))
        profileCreationViewModel.dispatch(ProfileCreationAction.UserSurnameWritten("Soler"))
        profileCreationViewModel.dispatch(ProfileCreationAction.CreateClicked)
        profileCreationViewModel.dispatch(ProfileCreationAction.DialogConfirmClicked)
        advanceUntilIdle()

        val marta = Friend("3", "Marta", "Soler")
        recorder.assertState { it.friends == listOf(ana, luis, marta) }
        recorder.assertEvents(HomeEvent.FriendCreated(marta))
        profileCreationViewModel.close()
        viewModel.close()
    }

    @Test
    fun `the view models listen to the broadcast actions they handle`() {
        homeViewModel().assertSetup()
        ProfileCreationViewModel(
            AddFriendUseCase(repository),
            ProfileCreationState.DEFAULT,
            EmaStateStore.None
        ).assertSetup()
    }
}
