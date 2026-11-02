package com.carmabs.emanate.compose.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.carmabs.emanate.android.extension.screenHeightPx
import com.carmabs.emanate.android.extension.screenWidthPx
import com.carmabs.emanate.core.utils.EmaScreenUtils

/**
 * Screen utilities of Compose. Use them through `EmaUtils.Screen.Compose`.
 *
 * @author <a href=“mailto:apps.carmabs@gmail.com”>Carlos Mateo Benito</a>
 */
object EmaComposeScreenUtils {

    /**
     * The width of the screen in pixels.
     */
    @Composable
    fun widthPx(): Float = LocalConfiguration.current.screenWidthPx

    /**
     * The height of the screen in pixels.
     */
    @Composable
    fun heightPx(): Float = LocalConfiguration.current.screenHeightPx

    /**
     * The width of the screen in Dp.
     */
    @Composable
    fun widthDp(): Dp = LocalConfiguration.current.screenWidthDp.dp

    /**
     * The height of the screen in Dp.
     */
    @Composable
    fun heightDp(): Dp = LocalConfiguration.current.screenHeightDp.dp
}

/**
 * Screen utilities of Compose, for example `EmaUtils.Screen.Compose.widthDp()`.
 */
val EmaScreenUtils.Compose: EmaComposeScreenUtils
    get() = EmaComposeScreenUtils
