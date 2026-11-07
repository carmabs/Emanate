package com.carmabs.emanate.ui.profile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import org.koin.android.ext.android.get
import org.koin.core.parameter.parametersOf
import com.carmabs.emanate.compose.nav2.emaScreen
import com.carmabs.emanate.compose.nav2.routeId
import com.carmabs.emanate.presentation.profile.creation.ProfileCreationViewModel
import com.carmabs.emanate.ui.profile.creation.ProfileCreationScreenContent
import com.carmabs.emanate.ui.theme.EmaSampleTheme

class ProfileActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            EmaSampleTheme {
                val navController = rememberNavController()
                val navigator = remember {
                    ProfileNavigator(navController)
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = ProfileCreationScreenContent::class.routeId
                    ) {
                        emaScreen(
                            screenContent = ProfileCreationScreenContent(),
                            viewModel = { store -> get<ProfileCreationViewModel> { parametersOf(store) } },
                            onEvent = {
                                navigator.handleProfileCreationEvent(it)
                            }
                        )
                    }
                }
            }
        }
    }
}
