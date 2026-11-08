package com.carmabs.emanate.ui.profile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.carmabs.emanate.core.action.EmaActionDispatcher
import com.carmabs.emanate.core.state.EmaState
import com.carmabs.emanate.presentation.profile.creation.ProfileCreationViewModel
import com.carmabs.emanate.ui.theme.EmaSampleTheme
import org.koin.android.ext.android.get
import org.koin.core.parameter.parametersOf

/**
 * Shows [ProfileScreenContent], the graph with the creation of a friend
 */
class ProfileActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val profileContent = ProfileScreenContent { store -> get<ProfileCreationViewModel> { parametersOf(store) } }
        setContent {
            EmaSampleTheme {
                // There is no view model behind the profile content, so it is drawn directly, without EmaScreen
                profileContent.onState(EmaState.EMPTY, EmaActionDispatcher.EMPTY)
            }
        }
    }
}
