package com.carmabs.emanate.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.width
import androidx.compose.ui.text.font.FontWeight
import com.carmabs.emanate.core.action.EmaActionDispatcher
import com.carmabs.emanate.core.model.EmaText
import com.carmabs.emanate.presentation.login.LoginAction
import com.carmabs.emanate.presentation.login.LoginDialog
import com.carmabs.emanate.presentation.login.LoginState
import com.carmabs.emanate.sample.ui.R
import com.carmabs.emanate.compose.ui.EmaScreenContent
import com.carmabs.emanate.ui.compose.AppButton
import com.carmabs.emanate.ui.compose.EnvironmentLabel
import com.carmabs.emanate.ui.compose.SimpleDialogComposable
import com.carmabs.emanate.ui.environment.AppEnvironment
import com.carmabs.emanate.ui.dialog.simple.SimpleDialogData
import com.carmabs.emanate.ui.utils.RenderPreview

/**
 * Login of the sample. The messages it shows are events handled where the graph is created.
 */
class LoginScreenContent(private val environment: AppEnvironment) : EmaScreenContent<LoginState, LoginAction> {

    @Composable
    override fun onState(state: LoginState, actions: EmaActionDispatcher<LoginAction>) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(dimensionResource(id = R.dimen.screen_padding))
            ) {
                // The header is centered: the brand first, so it is clear which app it is, and the welcome below it
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Image(
                        modifier = Modifier.size(88.dp),
                        painter = painterResource(id = R.drawable.ic_ema_logo),
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(id = R.string.ema_sample_name),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    // Below the name, like in the splash. The padding is part of the label, so the real build has no gap
                    EnvironmentLabel(
                        modifier = Modifier.padding(top = 8.dp),
                        environment = environment
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = stringResource(id = R.string.login_title),
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(id = R.string.login_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
                UserField(state, actions)
                Spacer(modifier = Modifier.height(16.dp))
                PasswordField(state, actions)
                Spacer(modifier = Modifier.height(24.dp))
                LoginButton(state, actions)
                Spacer(modifier = Modifier.height(32.dp))
                DemoCard()
            }
        }
        state.dialog?.also { StateDialog(it, actions) }
    }

    @Composable
    private fun UserField(state: LoginState, actions: EmaActionDispatcher<LoginAction>) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = state.userName,
            enabled = !state.isLoading,
            label = { Text(stringResource(id = R.string.login_user_hint)) },
            leadingIcon = {
                Icon(painter = painterResource(id = R.drawable.ic_person), contentDescription = null)
            },
            trailingIcon = {
                if (state.userName.isNotEmpty()) {
                    IconButton(onClick = { actions.dispatch(LoginAction.DeleteUser) }) {
                        Icon(painter = painterResource(id = R.drawable.ic_close), contentDescription = null)
                    }
                }
            },
            isError = state.userNameError,
            supportingText = errorText(state.userNameError, R.string.login_error_user_empty),
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            onValueChange = { actions.dispatch(LoginAction.UserNameWritten(it)) }
        )
    }

    @Composable
    private fun PasswordField(state: LoginState, actions: EmaActionDispatcher<LoginAction>) {
        var visible by remember { mutableStateOf(false) }
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = state.userPassword,
            enabled = !state.isLoading,
            label = { Text(stringResource(id = R.string.login_password_hint)) },
            leadingIcon = {
                Icon(painter = painterResource(id = R.drawable.ic_lock), contentDescription = null)
            },
            trailingIcon = {
                IconButton(onClick = { visible = !visible }) {
                    Icon(
                        painter = painterResource(id = if (visible) R.drawable.ic_visibility_off else R.drawable.ic_visibility),
                        contentDescription = null
                    )
                }
            },
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            isError = state.passwordError,
            supportingText = errorText(state.passwordError, R.string.login_error_password_empty),
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { actions.dispatch(LoginAction.Login) }),
            onValueChange = { actions.dispatch(LoginAction.PasswordWritten(it)) }
        )
    }

    @Composable
    private fun errorText(hasError: Boolean, message: Int): (@Composable () -> Unit)? =
        if (hasError) {
            { Text(text = stringResource(id = message)) }
        } else {
            null
        }

    @Composable
    private fun LoginButton(state: LoginState, actions: EmaActionDispatcher<LoginAction>) {
        Box(contentAlignment = Alignment.Center) {
            // The button is not disabled to keep its color behind the progress indicator
            AppButton(
                text = if (state.isLoading) "" else stringResource(id = R.string.login_access)
            ) {
                if (!state.isLoading) actions.dispatch(LoginAction.Login)
            }
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 3.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }

    @Composable
    private fun DemoCard() {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    modifier = Modifier.size(24.dp),
                    painter = painterResource(id = R.drawable.ic_info),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Column {
                    Text(
                        text = stringResource(id = R.string.login_demo_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = stringResource(id = R.string.login_demo_credentials),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }

    @Composable
    private fun StateDialog(dialog: LoginDialog, actions: EmaActionDispatcher<LoginAction>) {
        when (dialog) {
            LoginDialog.ErrorBadCredentials -> SimpleDialogComposable(
                dialogData = SimpleDialogData(
                    title = EmaText.id(R.string.login_error_fail_title),
                    message = EmaText.id(R.string.login_error_fail),
                    image = R.drawable.ic_error,
                    isError = true
                ),
                onConfirmClick = { actions.dispatch(LoginAction.Error.BadCredentialsAccepted) },
                onDismiss = { actions.dispatch(LoginAction.Error.BackPressed) }
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun PreviewNormal() {
    RenderPreview {
        LoginScreenContent(AppEnvironment("mock")).onState(state = LoginState.DEFAULT, actions = EmaActionDispatcher.EMPTY)
    }
}
