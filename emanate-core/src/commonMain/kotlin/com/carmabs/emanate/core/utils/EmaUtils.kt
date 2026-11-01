package com.carmabs.emanate.core.utils

/**
 * Created by Carlos Mateo Benito on 12/10/2026.
 *
 * Utility functions of Emanate, grouped by theme, so they can be found by writing `EmaUtils.`. The Android modules add
 * their own functions to the themes, under the name of the platform: for example `EmaUtils.Screen.Android` in
 * emanate-android and `EmaUtils.Screen.Compose` in emanate-compose-android.
 *
 * <p>
 * Copyright (c) 2026 by Carmabs. All rights reserved.
 * </p>
 *
 * @author <a href=“mailto:apps.carmabs@gmail.com”>Carlos Mateo Benito</a>
 */
object EmaUtils {

    /**
     * Coroutine utilities.
     */
    val Coroutines: EmaCoroutineUtils = EmaCoroutineUtils

    /**
     * Flow utilities.
     */
    val Flows: EmaFlowUtils = EmaFlowUtils

    /**
     * Utilities to walk the positions of a list in a different order.
     */
    val Positions: EmaPositionUtils = EmaPositionUtils

    /**
     * Size utilities.
     */
    val Size: EmaSizeUtils = EmaSizeUtils

    /**
     * Utilities to react to the changes of a value.
     */
    val Values: EmaValueUtils = EmaValueUtils

    /**
     * Screen utilities: `EmaUtils.Screen.Android` in emanate-android and `EmaUtils.Screen.Compose` in
     * emanate-compose-android.
     */
    val Screen: EmaScreenUtils = EmaScreenUtils

    /**
     * Color utilities: `EmaUtils.Color.Android` in emanate-android.
     */
    val Color: EmaColorUtils = EmaColorUtils

    /**
     * Utilities of the views: `EmaUtils.View.Android` in emanate-android-view.
     */
    val View: EmaViewUtils = EmaViewUtils

    /**
     * Utilities of the previews: `EmaUtils.Preview.Compose` in emanate-compose-android.
     */
    val Preview: EmaPreviewUtils = EmaPreviewUtils
}
