package com.carmabs.emanate.ui.profile.onboarding

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.carmabs.domain.model.User
import com.carmabs.emanate.compose.action.EmaImmutableActionDispatcher
import com.carmabs.emanate.presentation.profile.onboarding.ProfileOnBoardingActions
import com.carmabs.emanate.presentation.profile.onboarding.ProfileOnBoardingState
import com.carmabs.emanate.sample.ui.R
import com.carmabs.emanate.ui.base.compose.BaseScreenComposable
import com.carmabs.emanate.ui.theme.EmaSampleTheme

class ProfileOnBoardingScreenContent :
    BaseScreenComposable<ProfileOnBoardingState, ProfileOnBoardingActions>() {

    // System back follows the same flow as the back arrow
    override fun onBack(state: ProfileOnBoardingState): ProfileOnBoardingActions = ProfileOnBoardingActions.BackClicked

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun onState(
        state: ProfileOnBoardingState,
        actions: EmaImmutableActionDispatcher<ProfileOnBoardingActions>
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = { Text(text = stringResource(id = R.string.profile_panel)) },
                    navigationIcon = {
                        IconButton(onClick = { actions.dispatch(ProfileOnBoardingActions.BackClicked) }) {
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
                if (state.userName.isNotBlank()) {
                    Text(
                        text = stringResource(id = R.string.profile_admin, state.userName),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    modifier = Modifier.padding(top = 24.dp, bottom = 16.dp),
                    text = stringResource(id = R.string.profile_type_user_title),
                    style = MaterialTheme.typography.titleMedium
                )
                RoleCard(
                    icon = R.drawable.ic_admin,
                    title = R.string.role_admin,
                    description = R.string.profile_user_admin_description,
                    iconContainer = MaterialTheme.colorScheme.primaryContainer,
                    iconColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    actions.dispatch(ProfileOnBoardingActions.AdminClicked)
                }
                Spacer(modifier = Modifier.height(12.dp))
                RoleCard(
                    icon = R.drawable.ic_user,
                    title = R.string.role_basic,
                    description = R.string.profile_user_basic_description,
                    iconContainer = MaterialTheme.colorScheme.secondaryContainer,
                    iconColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    actions.dispatch(ProfileOnBoardingActions.UserClicked)
                }
            }
        }
    }

    @Composable
    private fun RoleCard(
        @DrawableRes icon: Int,
        @StringRes title: Int,
        @StringRes description: Int,
        iconContainer: Color,
        iconColor: Color,
        onClick: () -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = onClick,
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(iconContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = icon),
                        contentDescription = null,
                        tint = iconColor
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp)
                ) {
                    Text(
                        text = stringResource(id = title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(id = description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    painter = painterResource(id = R.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    @Preview
    @Composable
    private fun NormalPreview() {
        EmaSampleTheme {
            onState(
                state = ProfileOnBoardingState(
                    User(
                        "Carlos",
                        "Mateo"
                    )
                ),
                actions = EmaImmutableActionDispatcher.EMPTY
            )
        }
    }
}
