package com.carmabs.emanate.ui.home

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.carmabs.domain.model.Role
import com.carmabs.domain.model.User
import com.carmabs.emanate.core.action.EmaActionDispatcher
import com.carmabs.emanate.presentation.extension.fullName
import com.carmabs.emanate.presentation.extension.fullNameOf
import com.carmabs.emanate.presentation.extension.initials
import com.carmabs.emanate.presentation.extension.initialsOf
import com.carmabs.emanate.presentation.home.HomeAction
import com.carmabs.emanate.presentation.home.HomeState
import com.carmabs.emanate.sample.ui.R
import com.carmabs.emanate.compose.ui.EmaScreenContent
import com.carmabs.emanate.ui.utils.RenderPreview

/**
 * Home of the sample: the profile of the user and the list of users, different for each role
 */
class HomeScreenContent : EmaScreenContent<HomeState, HomeAction> {

    // The system back and the close button take the same path through the view model
    override fun onBack(state: HomeState): HomeAction = HomeAction.CloseClicked

    @Composable
    override fun onState(state: HomeState, actions: EmaActionDispatcher<HomeAction>) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            floatingActionButton = {
                if (state.showCreateButton) {
                    ExtendedFloatingActionButton(
                        text = { Text(stringResource(id = R.string.home_create)) },
                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_person_add),
                                contentDescription = null
                            )
                        },
                        onClick = { actions.dispatch(HomeAction.CreateClicked) }
                    )
                }
            }
        ) { padding ->
            val userData = state.userData
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (userData != null) {
                    Content(state, userData, actions)
                }
            }
        }
    }

    @Composable
    private fun Content(
        state: HomeState,
        userData: HomeState.UserData,
        actions: EmaActionDispatcher<HomeAction>
    ) {
        val isAdmin = userData.role == Role.ADMIN
        val screenPadding = dimensionResource(id = R.dimen.screen_padding)
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.padding(start = screenPadding, end = 4.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = stringResource(id = R.string.home_greeting, userData.name),
                    style = MaterialTheme.typography.headlineMedium
                )
                IconButton(onClick = { actions.dispatch(HomeAction.CloseClicked) }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_close),
                        contentDescription = stringResource(id = R.string.home_close)
                    )
                }
            }
            Text(
                modifier = Modifier.padding(start = screenPadding, end = screenPadding, top = 4.dp),
                text = stringResource(
                    id = if (isAdmin) R.string.home_subtitle_admin else R.string.home_subtitle_basic
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ProfileCard(userData, isAdmin)
            Text(
                modifier = Modifier.padding(start = screenPadding, end = screenPadding, top = 24.dp),
                text = stringResource(
                    id = if (isAdmin) R.string.home_section_admin else R.string.home_section_basic
                ),
                style = MaterialTheme.typography.titleMedium
            )
            if (state.showEmptyList) {
                EmptyList(isAdmin)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = screenPadding,
                        end = screenPadding,
                        top = 12.dp,
                        bottom = 96.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.userList, key = { it.id.ifEmpty { it.fullName } }) { user ->
                        UserItem(
                            // The removed user fades out while the ones below slide up to its place
                            modifier = Modifier.animateItem(),
                            user = user,
                            showDelete = state.showDeleteButtons
                        ) {
                            actions.dispatch(HomeAction.DeleteClicked(user))
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ProfileCard(userData: HomeState.UserData, isAdmin: Boolean) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(id = R.dimen.screen_padding))
                .padding(top = 24.dp),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Avatar(
                    text = initialsOf(userData.name, userData.surname),
                    container = MaterialTheme.colorScheme.primary,
                    content = MaterialTheme.colorScheme.onPrimary,
                    size = 56.dp
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp)
                ) {
                    Text(
                        text = fullNameOf(userData.name, userData.surname),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    RoleChip(
                        role = if (isAdmin) R.string.role_admin else R.string.role_basic,
                        container = MaterialTheme.colorScheme.surfaceContainerLowest,
                        content = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    @Composable
    private fun UserItem(modifier: Modifier, user: User, showDelete: Boolean, onDelete: () -> Unit) {
        val isAdmin = user.role == Role.ADMIN
        val container = if (isAdmin) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        }
        val content = if (isAdmin) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSecondaryContainer
        }
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Avatar(text = user.initials, container = container, content = content, size = 44.dp)
                Text(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    text = user.fullName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                RoleChip(
                    role = if (isAdmin) R.string.role_admin else R.string.role_basic,
                    container = container,
                    content = content
                )
                if (showDelete) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_delete),
                            contentDescription = stringResource(id = R.string.home_delete),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun Avatar(text: String, container: Color, content: Color, size: Dp) {
        Box(
            modifier = Modifier
                .size(size)
                .background(container, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = text, style = MaterialTheme.typography.titleMedium, color = content)
        }
    }

    @Composable
    private fun RoleChip(@StringRes role: Int, container: Color, content: Color) {
        Surface(shape = CircleShape, color = container, contentColor = content) {
            Text(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                text = stringResource(id = role),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }

    @Composable
    private fun EmptyList(isAdmin: Boolean) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = dimensionResource(id = R.dimen.screen_padding))
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier.size(32.dp),
                    painter = painterResource(id = R.drawable.ic_group),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(id = R.string.home_empty_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = stringResource(
                    id = if (isAdmin) R.string.home_empty_message_admin else R.string.home_empty_message_basic
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }

    @PreviewLightDark
    @Composable
    private fun PreviewAdmin() {
        RenderPreview {
            onState(
                state = HomeState(
                    userData = HomeState.UserData("Carlos", "Mateo", Role.ADMIN),
                    userList = listOf(User("Ana", "López", Role.BASIC, "1"), User("Luis", "Pérez", Role.BASIC, "2")),
                    friendsLoaded = true
                ),
                actions = EmaActionDispatcher.EMPTY
            )
        }
    }

    @PreviewLightDark
    @Composable
    private fun PreviewEmpty() {
        RenderPreview {
            onState(
                state = HomeState(HomeState.UserData("Carlos", "Mateo", Role.BASIC), emptyList(), true),
                actions = EmaActionDispatcher.EMPTY
            )
        }
    }

    @PreviewLightDark
    @Composable
    private fun PreviewEmptyAdmin() {
        RenderPreview {
            onState(
                state = HomeState(HomeState.UserData("Carlos", "Mateo", Role.ADMIN), emptyList(), true),
                actions = EmaActionDispatcher.EMPTY
            )
        }
    }
}
