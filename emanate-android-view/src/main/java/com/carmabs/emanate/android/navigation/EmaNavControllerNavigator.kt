@file:Suppress("DEPRECATION")

package com.carmabs.emanate.android.navigation

import android.app.Activity
import android.os.Bundle
import androidx.annotation.IdRes
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.FragmentNavigator
import com.carmabs.emanate.core.navigator.EmaNavigator
import com.carmabs.emanate.core.state.EmaEvent

/**
 *  *<p>
 * Copyright (c) 2020, Carmabs. All rights reserved.
 * </p>
 *
 * @author <a href=“mailto:apps.carmabs@gmail.com”>Carlos Mateo Benito</a>
 *
 * Navigator to handle navigation through navController with navigation back support and no target navigation
 * Created by: Carlos Mateo Benito on 20/1/19.
 */
@Deprecated(
    "Emanate will work only with Compose and EmaViewModel. Build the screens with emanate-compose-android and EmaViewModel instead of Views."
)
interface EmaNavControllerNavigator<E : EmaEvent> : EmaNavigator<E> {

    val navController: NavController

    val activity: Activity

    /**
     * Navigate with android architecture components within action ID
     * @param actionID
     * @param data
     * @param navOptions
     */
    fun navigateWithAction(
        @IdRes actionID: Int,
        data: Bundle? = null,
        navOptions: NavOptions? = null,
        extras: FragmentNavigator.Extras? = null
    ) {
        navController.navigate(actionID, data, navOptions, extras)
    }

    /**
     * Navigates back
     * @return true if a destination was popped, false otherwise
     */
    override fun navigateBack(result: Any?): Boolean = navController.popBackStack()
}
