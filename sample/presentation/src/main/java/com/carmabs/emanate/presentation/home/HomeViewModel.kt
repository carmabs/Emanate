package com.carmabs.emanate.presentation.home

import com.carmabs.domain.model.User
import com.carmabs.domain.usecase.GetUserFriendsUseCase
import com.carmabs.emanate.core.viewmodel.EmaViewModel
import com.carmabs.emanate.presentation.profile.creation.ProfileCreationBroadcast

class HomeViewModel(private val getUserFriendsUseCase: GetUserFriendsUseCase, initialDataState: HomeState) :
    EmaViewModel<HomeState, HomeAction, HomeEvent>(initialDataState) {

    override fun setup() = source {
        reducers {
            add<HomeInitializer> { initializer ->
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
            add<HomeFriendsLoaded> { action -> state.copy(userList = state.userList + action.friends) }
            add<ProfileCreationBroadcast.UserCreated> { action ->
                state.copy(userList = state.userList + action.user)
            }
        }

        sideEffects {
            add<HomeInitializer> { initializer ->
                when (initializer) {
                    is HomeInitializer.HomeUser -> {
                        val friends = getUserFriendsUseCase(GetUserFriendsUseCase.Input(initializer.user))
                        dispatch(HomeFriendsLoaded(friends))
                    }
                }
            }
            add<HomeAction.ProfileClicked> {
                state.userData?.also { publish(HomeEvent.ProfileClicked(User(it.name, it.surname, it.role))) }
            }
            // The login screen shows the last user added
            add<ProfileCreationBroadcast.UserCreated> { action ->
                broadcast(HomeBroadcast.UserAdded(action.user), keepUntilReceived = true)
            }
        }

        broadcasts {
            // The users created in the profile screen
            listen<ProfileCreationBroadcast.UserCreated>()
        }
    }
}
