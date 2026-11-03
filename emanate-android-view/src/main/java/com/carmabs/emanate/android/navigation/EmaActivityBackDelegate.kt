@file:Suppress("DEPRECATION")

package com.carmabs.emanate.android.navigation

import com.carmabs.emanate.core.model.EmaBackHandlerStrategy

/**
 * Created by Carlos Mateo Benito on 12/1/24.
 *
 * <p>
 * Copyright (c) 2024 by Carmabs. All rights reserved.
 * </p>
 *
 * @author <a href=“mailto:apps.carmabs@gmail.com”>Carlos Mateo Benito</a>
 */
@Deprecated(
    "Emanate will work only with Compose and EmaViewModel. Build the screens with emanate-compose-android and EmaViewModel instead of Views."
)
interface EmaActivityBackDelegate {

    val ownsBackDelegate: Boolean
    fun onBackDelegate(): EmaBackHandlerStrategy
}
