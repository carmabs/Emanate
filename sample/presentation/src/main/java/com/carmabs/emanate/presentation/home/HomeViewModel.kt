package com.carmabs.emanate.presentation.home

import com.carmabs.domain.usecase.DeleteFriendUseCase
import com.carmabs.domain.usecase.GetFriendsUseCase
import com.carmabs.emanate.core.model.onFailure
import com.carmabs.emanate.core.model.onSuccess
import com.carmabs.emanate.core.source.reducer.reducerOf
import com.carmabs.emanate.core.source.sideeffect.sideEffectOf
import com.carmabs.emanate.presentation.base.BaseViewModel
import com.carmabs.emanate.presentation.profile.creation.ProfileCreationBroadcast

class HomeViewModel(
    private val getFriendsUseCase: GetFriendsUseCase,
    private val deleteFriendUseCase: DeleteFriendUseCase,
    initialDataState: HomeState
) : BaseViewModel<HomeState, HomeAction, HomeEvent>(initialDataState) {

    private val reducerInitializer = reducerOf(HomeInitializer::class) { initializer ->
        when (initializer) {
            is HomeInitializer.HomeUser -> state.copy(
                userData = HomeState.UserData(
                    name = initializer.user.name,
                    surname = initializer.user.surname,
                    role = initializer.user.role
                )
            )
        }
    }

    private val reducerAction = reducerOf(HomeAction::class) { action ->
        when (action) {
            // The user disappears from the list at once, the side effect deletes it from the storage
            is HomeAction.DeleteClicked -> state.copy(friends = state.friends.filterNot { it.id == action.friend.id })

            HomeAction.CreateClicked,
            HomeAction.CloseClicked -> state

            // They are reduced by reducerSideEffects
            is HomeAction.SideEffects -> state
        }
    }

    private val reducerSideEffects = reducerOf(HomeAction.SideEffects::class) { action ->
        when (action) {
            is HomeAction.SideEffects.FriendsLoaded -> state.copy(friends = action.friends, friendsLoaded = true)
        }
    }

    // The friends are read when the screen is created
    private val sideEffectLoadFriends = sideEffectOf(HomeInitializer::class) { initializer ->
        when (initializer) {
            is HomeInitializer.HomeUser -> getFriendsUseCase(Unit)
                .onSuccess { dispatch(HomeAction.SideEffects.FriendsLoaded(it)) }
                .onFailure { publish(HomeEvent.LoadFailed) }
        }
    }

    private val sideEffectOpenCreation = sideEffectOf(HomeAction.CreateClicked::class) {
        publish(HomeEvent.CreateClicked)
    }

    private val sideEffectDeleteFriend = sideEffectOf(HomeAction.DeleteClicked::class) { action ->
        deleteFriendUseCase(DeleteFriendUseCase.Input(action.friend.id)).onFailure {
            // The friend left the list at once, so the storage, where it still is, gives it back its place
            publish(HomeEvent.DeleteFailed(action.friend))
            getFriendsUseCase(Unit)
                .onSuccess { dispatch(HomeAction.SideEffects.FriendsLoaded(it)) }
                .onFailure { publish(HomeEvent.LoadFailed) }
        }
    }

    private val sideEffectClose = sideEffectOf(HomeAction.CloseClicked::class) {
        publish(HomeEvent.CloseClicked)
    }

    // The screen of the profile creation is in another activity, so the list is read again from the storage
    private val sideEffectReloadFriends = sideEffectOf(ProfileCreationBroadcast.FriendCreated::class) { action ->
        getFriendsUseCase(Unit)
            .onSuccess { dispatch(HomeAction.SideEffects.FriendsLoaded(it)) }
            .onFailure { publish(HomeEvent.LoadFailed) }
        publish(HomeEvent.FriendCreated(action.friend))
    }

    override fun setup() = source {
        reducers {
            add(reducerInitializer, reducerAction, reducerSideEffects)
        }

        sideEffects {
            add(
                sideEffectLoadFriends,
                sideEffectOpenCreation,
                sideEffectDeleteFriend,
                sideEffectClose,
                sideEffectReloadFriends
            )
        }

        broadcasts {
            // The friends created in the profile screen
            listen<ProfileCreationBroadcast.FriendCreated>()
        }
    }
}
