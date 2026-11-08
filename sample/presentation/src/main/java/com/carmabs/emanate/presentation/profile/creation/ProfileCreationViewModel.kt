package com.carmabs.emanate.presentation.profile.creation

import com.carmabs.domain.usecase.AddFriendUseCase
import com.carmabs.emanate.core.model.onFailure
import com.carmabs.emanate.core.model.onSuccess
import com.carmabs.emanate.core.savestate.EmaStateStore
import com.carmabs.emanate.core.savestate.backupOf
import com.carmabs.emanate.core.source.reducer.reducerOf
import com.carmabs.emanate.core.source.sideeffect.sideEffectOf
import com.carmabs.emanate.presentation.base.BaseViewModel
import com.carmabs.emanate.serialization.json

class ProfileCreationViewModel(
    private val addFriendUseCase: AddFriendUseCase,
    initialDataState: ProfileCreationState,
    private val stateStore: EmaStateStore
) :
    BaseViewModel<ProfileCreationState, ProfileCreationAction, ProfileCreationEvent>(initialDataState) {

    private val reducerAction = reducerOf(ProfileCreationAction::class) { action ->
        when (action) {
            is ProfileCreationAction.UserNameWritten -> state.copy(name = action.name)

            is ProfileCreationAction.UserSurnameWritten -> state.copy(surname = action.surname)

            ProfileCreationAction.CreateClicked -> if (state.canCreate) {
                state.copy(dialog = ProfileCreationDialog.CreateConfirmation)
            } else {
                state
            }

            // The back press asks for confirmation, unless a dialog is already shown
            ProfileCreationAction.OnBack -> if (state.dialog == null) {
                state.copy(dialog = ProfileCreationDialog.BackConfirmation)
            } else {
                state
            }

            ProfileCreationAction.DialogConfirmClicked,
            ProfileCreationAction.DialogCancelClicked,
            ProfileCreationAction.DialogBackConfirm,
            ProfileCreationAction.DialogBackCancel,
            ProfileCreationAction.DialogErrorAccepted -> state.copy(dialog = null)

            // They are reduced by reducerSideEffects
            is ProfileCreationAction.SideEffects -> state
        }
    }

    private val reducerSideEffects = reducerOf(ProfileCreationAction.SideEffects::class) { action ->
        when (action) {
            ProfileCreationAction.SideEffects.CreateFailed -> state.copy(dialog = ProfileCreationDialog.CreateError)
        }
    }

    // The friend is stored and the home screen, which lists the stored friends, is told to read them again
    private val sideEffectCreateUser = sideEffectOf(ProfileCreationAction.DialogConfirmClicked::class) {
        addFriendUseCase(AddFriendUseCase.Input(state.name, state.surname))
            .onSuccess { friend ->
                broadcast(ProfileCreationBroadcast.FriendCreated(friend), keepUntilReceived = true)
                publish(ProfileCreationEvent.DialogConfirmationAccepted)
            }
            // The screen stays open with the form, so the user can try again
            .onFailure { dispatch(ProfileCreationAction.SideEffects.CreateFailed) }
    }

    private val sideEffectCloseAfterBack = sideEffectOf(ProfileCreationAction.DialogBackConfirm::class) {
        publish(ProfileCreationEvent.DialogConfirmationAccepted)
    }

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
            add(reducerAction, reducerSideEffects)
        }

        sideEffects {
            add(sideEffectCreateUser, sideEffectCloseAfterBack)
        }
    }
}
