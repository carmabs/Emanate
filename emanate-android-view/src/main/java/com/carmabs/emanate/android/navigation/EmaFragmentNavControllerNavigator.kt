@file:Suppress("DEPRECATION")

package com.carmabs.emanate.android.navigation

import android.app.Activity
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
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
 *  Navigator to handle navigation through navController in a Fragment
 * Created by: Carlos Mateo Benito on 29/07/22.
 */
@Deprecated(
    "Emanate will work only with Compose and EmaViewModel. Build the screens with emanate-compose-android and EmaViewModel instead of Views."
)
abstract class EmaFragmentNavControllerNavigator<E : EmaEvent>(private val fragment: Fragment) :
    EmaNavControllerNavigator<E> {

    private val gson by lazy {
        Gson()
    }

    final override val activity: Activity by lazy {
        fragment.requireActivity()
    }

    final override val navController: NavController by lazy {
        fragment.findNavController()
    }
}
