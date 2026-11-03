@file:Suppress("DEPRECATION")

package com.carmabs.emanate.android.delegates

import kotlin.reflect.KProperty

/**
 * Created by Carlos Mateo Benito on 13/02/2021.
 *
 * <p>
 * Copyright (c) 2021 by Carmabs. All rights reserved.
 * </p>
 *
 * @author <a href=“mailto:apps.carmabs@gmail.com”>Carlos Mateo Benito</a>
 */
@Suppress("ClassName")
@Deprecated(
    "Emanate will work only with Compose and EmaViewModel. Build the screens with emanate-compose-android and EmaViewModel instead of Views."
)
class emaStateDelegate<T>(private val initialStateCreation: () -> T) {

    private var state: T? = null

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = state ?: initialStateCreation.invoke().apply {
        state = this
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        state = value
    }
}
