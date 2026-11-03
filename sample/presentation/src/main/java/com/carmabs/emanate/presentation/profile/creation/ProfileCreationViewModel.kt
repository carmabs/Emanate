package com.carmabs.emanate.presentation.profile.creation

import com.carmabs.domain.model.Role
import com.carmabs.domain.model.User
import com.carmabs.emanate.core.savestate.EmaStateStore
import com.carmabs.emanate.core.savestate.backupOf
import com.carmabs.emanate.core.viewmodel.EmaViewModel
import com.carmabs.emanate.serialization.json

class ProfileCreationViewModel(initialDataState: ProfileCreationState, private val stateStore: EmaStateStore) :
    EmaViewModel<ProfileCreationState, ProfileCreationAction, ProfileCreationEvent>(initialDataState) {

    override fun setup() = source {
        // The form is restored after the system kills the process, for example while the user changes a permission
        // in the settings
        plugins {
            add(backupOf(stateStore) {
                add(
                    key = "form",
                    codec = json(),
                    save = { ProfileCreationForm(name, surname) },
                    restore = { form -> copy(name = form.name, surname = form.surname) }
                )
            })
        }

        reducers {
            add<ProfileCreationInitializer> { initializer ->
                when (initializer) {
                    ProfileCreationInitializer.Admin -> state.copy(role = Role.ADMIN)
                    ProfileCreationInitializer.UserBasic -> state.copy(role = Role.BASIC)
                }
            }

            add<ProfileCreationAction> { action ->
                when (action) {
                    is ProfileCreationAction.UserNameWritten -> state.copy(name = action.name)

                    is ProfileCreationAction.UserSurnameWritten -> state.copy(surname = action.surname)

                    ProfileCreationAction.CreateClicked -> if (state.canCreate) {
                        state.copy(dialog = ProfileCreationDialog.UserCreated(state.role))
                    } else {
                        state
                    }

                    ProfileCreationAction.OnBack -> state.copy(dialog = ProfileCreationDialog.BackConfirmation)

                    ProfileCreationAction.DialogConfirmClicked,
                    ProfileCreationAction.DialogCancelClicked,
                    ProfileCreationAction.DialogBackConfirm,
                    ProfileCreationAction.DialogBackCancel -> state.copy(dialog = null)
                }
            }
        }

        sideEffects {
            // The home screen adds the user to its list
            add<ProfileCreationAction.DialogConfirmClicked> {
                val user = User(state.name, state.surname, state.role)
                broadcast(ProfileCreationBroadcast.UserCreated(user), keepUntilReceived = true)
                publish(ProfileCreationEvent.DialogConfirmationAccepted)
            }

            add<ProfileCreationAction.DialogBackConfirm> {
                publish(ProfileCreationEvent.DialogConfirmationAccepted)
            }
        }
    }
}
