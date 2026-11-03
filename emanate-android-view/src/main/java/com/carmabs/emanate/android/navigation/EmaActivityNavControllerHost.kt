@file:Suppress("DEPRECATION")

package com.carmabs.emanate.android.navigation

import android.app.Activity
import android.content.Intent
import com.carmabs.emanate.android.constants.EMA_RESULT_CODE
import com.carmabs.emanate.android.constants.EMA_RESULT_KEY
import com.carmabs.emanate.core.state.EmaEvent
import com.google.gson.Gson

/**
 * Created by Carlos Mateo Benito on 29/7/22.
 *
 * <p>
 * Copyright (c) 2022 by Carmabs. All rights reserved.
 * </p>
 *
 * @author <a href=“mailto:apps.carmabs@gmail.com”>Carlos Mateo Benito</a>
 *
 * Navigator to handle navigation through navController in an Activity.
 * It acts like a fragment host that navigates only through its child fragments.
 * Created by: Carlos Mateo Benito on 29/07/22.
 */
@Deprecated(
    "Emanate will work only with Compose and EmaViewModel. Build the screens with emanate-compose-android and EmaViewModel instead of Views."
)
class EmaActivityNavControllerHost(activity: Activity, navHostId: Int, graphId: Int) :
    EmaActivityNavControllerNavigator<EmaEvent.EMPTY>(activity, navHostId, graphId) {

    private val gson by lazy {
        Gson()
    }

    override fun navigate(event: EmaEvent.EMPTY) = Unit

    /**
     * Navigates back
     * @return true if a fragment has been popped, false if backstack is empty, in that case, finish
     * the activity provided.
     */
    override fun navigateBack(result: Any?): Boolean {
        val hasMoreFragments = navController.popBackStack()
        if (!hasMoreFragments) {
            result?.also {
                activity.setResult(
                    EMA_RESULT_CODE,
                    Intent().putExtra(EMA_RESULT_KEY, gson.toJson(it))
                )
            }
            activity.finish()
        }
        return hasMoreFragments
    }
}
