@file:Suppress("DEPRECATION")

package com.carmabs.emanate.android.navigation

import android.app.Activity
import android.content.Intent
import androidx.navigation.NavController
import com.carmabs.emanate.android.constants.EMA_RESULT_CODE
import com.carmabs.emanate.android.constants.EMA_RESULT_KEY
import com.carmabs.emanate.core.navigator.EmaNavigator
import com.carmabs.emanate.core.state.EmaEvent
import com.google.gson.Gson

@Deprecated(
    "Emanate will work only with Compose and EmaViewModel. Build the screens with emanate-compose-android and EmaViewModel instead of Views."
)
data object EmaEmptyNavigationEvent : EmaEvent

@Deprecated(
    "Emanate will work only with Compose and EmaViewModel. Build the screens with emanate-compose-android and EmaViewModel instead of Views."
)
class EmaEmptyNavigator constructor(private val activity: Activity, private val navController: NavController) :
    EmaNavigator<EmaEmptyNavigationEvent> {

    private val gson by lazy {
        Gson()
    }

    override fun navigate(event: EmaEmptyNavigationEvent) = Unit

    override fun navigateBack(result: Any?): Boolean {
        val hasMoreBackScreens = navController.popBackStack()
        if (!hasMoreBackScreens) {
            result?.also {
                activity.setResult(
                    EMA_RESULT_CODE,
                    Intent().putExtra(EMA_RESULT_KEY, gson.toJson(it))
                )
            }
            activity.finish()
        }

        return hasMoreBackScreens
    }
}
