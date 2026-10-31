package com.carmabs.emanate.ui.profile.creation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.carmabs.domain.model.Role
import com.carmabs.emanate.compose.action.EmaImmutableActionDispatcher
import com.carmabs.emanate.core.model.EmaText
import com.carmabs.emanate.presentation.profile.creation.ProfileCreationAction
import com.carmabs.emanate.presentation.profile.creation.ProfileCreationOverlap
import com.carmabs.emanate.presentation.profile.creation.ProfileCreationState
import com.carmabs.emanate.sample.ui.R
import com.carmabs.emanate.ui.base.compose.BaseScreenComposable
import com.carmabs.emanate.ui.compose.AppButton
import com.carmabs.emanate.ui.dialog.simple.SimpleDialogData
import com.carmabs.emanate.ui.dialog.simple.SimpleDialogListener
import com.carmabs.emanate.ui.theme.EmaSampleTheme

class ProfileCreationScreenContent :
    BaseScreenComposable<ProfileCreationState, ProfileCreationAction>() {

    // When a dialog is shown, it handles the back press itself
    override fun onBack(state: ProfileCreationState): ProfileCreationAction? =
        if (state.overlap == null) ProfileCreationAction.OnBack else null

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun onState(state: ProfileCreationState, actions: EmaImmutableActionDispatcher<ProfileCreationAction>) {
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
                RoleChip(role = state.role)
                Spacer(modifier = Modifier.height(24.dp))
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

        state.overlap?.also {
            Overlap(it, actions)
        }
    }

    @Composable
    private fun RoleChip(role: Role) {
        val (container, content) = when (role) {
            Role.ADMIN -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
            Role.BASIC -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        }
        Surface(shape = CircleShape, color = container, contentColor = content) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    modifier = Modifier.size(18.dp),
                    painter = painterResource(id = role.icon),
                    contentDescription = null
                )
                Text(
                    modifier = Modifier.padding(start = 6.dp),
                    text = stringResource(id = role.title),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }

    @Composable
    private fun Overlap(overlap: ProfileCreationOverlap, actions: EmaImmutableActionDispatcher<ProfileCreationAction>) {
        when (overlap) {
            ProfileCreationOverlap.DialogBackConfirmation -> {
                ShowDialog(
                    data = SimpleDialogData(
                        title = EmaText.id(R.string.profile_creation_user_exit_title),
                        message = EmaText.id(R.string.profile_creation_user_exit_message),
                        showCancel = true,
                        image = R.drawable.ic_exit
                    ),
                    listener = object : SimpleDialogListener {
                        override fun onCancelClicked() {
                            actions.dispatch(ProfileCreationAction.DialogBackCancel)
                        }

                        override fun onConfirmClicked() {
                            actions.dispatch(ProfileCreationAction.DialogBackConfirm)
                        }

                        override fun onBackPressed() {
                            actions.dispatch(ProfileCreationAction.DialogBackCancel)
                        }
                    }
                )
            }

            is ProfileCreationOverlap.DialogUserCreated -> {
                val title = when (overlap.role) {
                    Role.ADMIN -> R.string.profile_creation_user_admin_title
                    Role.BASIC -> R.string.profile_creation_user_basic_title
                }
                ShowDialog(
                    data = SimpleDialogData(
                        title = EmaText.id(title),
                        message = EmaText.id(R.string.profile_creation_user_message),
                        showCancel = true,
                        image = overlap.role.icon
                    ),
                    listener = object : SimpleDialogListener {
                        override fun onCancelClicked() {
                            actions.dispatch(ProfileCreationAction.DialogCancelClicked)
                        }

                        override fun onConfirmClicked() {
                            actions.dispatch(ProfileCreationAction.DialogConfirmClicked)
                        }

                        override fun onBackPressed() {
                            actions.dispatch(ProfileCreationAction.DialogCancelClicked)
                        }
                    }
                )
            }
        }
    }

    private val Role.icon
        get() = when (this) {
            Role.ADMIN -> R.drawable.ic_admin
            Role.BASIC -> R.drawable.ic_user
        }

    private val Role.title
        get() = when (this) {
            Role.ADMIN -> R.string.role_admin
            Role.BASIC -> R.string.role_basic
        }

    @Preview
    @Composable
    private fun NormalPreview() {
        EmaSampleTheme {
            onState(
                state = ProfileCreationState(
                    Role.ADMIN,
                    "Carlos",
                    "Mateo"
                ),
                actions = EmaImmutableActionDispatcher.EMPTY
            )
        }
    }

    @Preview
    @Composable
    private fun OverlappedPreview() {
        EmaSampleTheme {
            onState(
                state = ProfileCreationState(
                    Role.ADMIN,
                    "Carlos",
                    "Mateo",
                    ProfileCreationOverlap.DialogUserCreated(Role.ADMIN)
                ),
                actions = EmaImmutableActionDispatcher.EMPTY
            )
        }
    }
}
