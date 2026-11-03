@file:Suppress("DEPRECATION")

package com.carmabs.emanate.android.utils

import android.view.View
import com.carmabs.emanate.core.utils.EmaUtils
import com.carmabs.emanate.core.utils.EmaViewUtils

/**
 * Utilities of the Android views. Use them through `EmaUtils.View.Android`.
 *
 * @author <a href=“mailto:apps.carmabs@gmail.com”>Carlos Mateo Benito</a>
 */
@Deprecated(
    "Emanate will work only with Compose and EmaViewModel. Build the screens with emanate-compose-android and EmaViewModel instead of Views."
)
object EmaAndroidViewUtils {

    /**
     * Returns [View.VISIBLE] if [visible] is true. Otherwise, [View.GONE], or [View.INVISIBLE] if [gone] is false.
     */
    fun visibility(visible: Boolean, gone: Boolean = true): Int = when {
        visible -> View.VISIBLE
        gone -> View.GONE
        else -> View.INVISIBLE
    }
}

/**
 * Utilities of the Android views, for example `EmaUtils.View.Android.visibility(isLoading)`.
 */
val EmaViewUtils.Android: EmaAndroidViewUtils
    get() = EmaAndroidViewUtils
