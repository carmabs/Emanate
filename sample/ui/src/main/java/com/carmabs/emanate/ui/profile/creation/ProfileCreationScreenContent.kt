package com.carmabs.emanate.ui.profile.creation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.carmabs.emanate.core.action.EmaActionDispatcher
import com.carmabs.emanate.core.model.EmaText
import com.carmabs.emanate.presentation.profile.creation.ProfileCreationAction
import com.carmabs.emanate.presentation.profile.creation.ProfileCreationDialog
import com.carmabs.emanate.presentation.profile.creation.ProfileCreationState
import com.carmabs.emanate.sample.ui.R
import com.carmabs.emanate.compose.ui.EmaScreenContent
import com.carmabs.emanate.ui.compose.AppButton
import com.carmabs.emanate.ui.compose.SimpleDialogComposable
import com.carmabs.emanate.ui.dialog.simple.SimpleDialogData
import com.carmabs.emanate.ui.utils.RenderPreview

class ProfileCreationScreenContent : EmaScreenContent<ProfileCreationState, ProfileCreationAction> {

    // The view only reports the back press: the ViewModel decides what it means
    override fun onBack(state: ProfileCreationState): ProfileCreationAction = ProfileCreationAction.OnBack

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun onState(state: ProfileCreationState, actions: EmaActionDispatcher<ProfileCreationAction>) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = { Text(text = stringResource(id = R.string.profile_creation_title)) },
                    navigationIcon = {
                        IconButton(onClick = { actions.dispatch(ProfileCreationAction.OnBack) }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_arrow_back),
                                contentDescription = stringResource(id = R.string.navigate_back)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = state.name,
                    label = {
                        Text(stringResource(id = R.string.profile_creation_create_name))
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_person),
                            contentDescription = null
                        )
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    ),
                    onValueChange = {
                        actions.dispatch(ProfileCreationAction.UserNameWritten(it))
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = state.surname,
                    label = {
                        Text(stringResource(id = R.string.profile_creation_create_surname))
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_person),
                            contentDescription = null
                        )
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        actions.dispatch(ProfileCreationAction.CreateClicked)
                    }),
                    onValueChange = {
                        actions.dispatch(ProfileCreationAction.UserSurnameWritten(it))
                    }
                )
                Spacer(modifier = Modifier.height(32.dp))
                AppButton(
                    text = stringResource(id = R.string.profile_creation_create_user),
                    enabled = state.canCreate
                ) {
                    actions.dispatch(ProfileCreationAction.CreateClicked)
                }
            }
        }

        state.dialog?.also {
            StateDialog(it, actions)
        }
    }

    @Composable
    private fun StateDialog(dialog: ProfileCreationDialog, actions: EmaActionDispatcher<ProfileCreationAction>) {
        when (dialog) {
            ProfileCreationDialog.BackConfirmation -> SimpleDialogComposable(
                dialogData = SimpleDialogData(
                    title = EmaText.id(R.string.profile_creation_user_exit_title),
                    message = EmaText.id(R.string.profile_creation_user_exit_message),
                    image = R.drawable.ic_exit
                ),
                onConfirmClick = { actions.dispatch(ProfileCreationAction.DialogBackConfirm) },
                onCancelClick = { actions.dispatch(ProfileCreationAction.DialogBackCancel) },
                onDismiss = { actions.dispatch(ProfileCreationAction.DialogBackCancel) }
            )

            ProfileCreationDialog.CreateConfirmation -> SimpleDialogComposable(
                dialogData = SimpleDialogData(
                    title = EmaText.id(R.string.profile_creation_user_title),
                    message = EmaText.id(R.string.profile_creation_user_message),
                    image = R.drawable.ic_user
                ),
                onConfirmClick = { actions.dispatch(ProfileCreationAction.DialogConfirmClicked) },
                onCancelClick = { actions.dispatch(ProfileCreationAction.DialogCancelClicked) },
                onDismiss = { actions.dispatch(ProfileCreationAction.DialogCancelClicked) }
            )
        }
    }

    @PreviewLightDark
    @Composable
    private fun PreviewNormal() {
        RenderPreview {
            onState(
                state = ProfileCreationState(
                    "Carlos",
                    "Mateo"
                ),
                actions = EmaActionDispatcher.EMPTY
            )
        }
    }

    @PreviewLightDark
    @Composable
    private fun PreviewDialog() {
        RenderPreview {
            onState(
                state = ProfileCreationState(
                    "Carlos",
                    "Mateo",
                    ProfileCreationDialog.CreateConfirmation
                ),
                actions = EmaActionDispatcher.EMPTY
            )
        }
    }
}
