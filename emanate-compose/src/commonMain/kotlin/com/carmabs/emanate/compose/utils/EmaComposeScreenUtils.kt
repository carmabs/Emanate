package com.carmabs.emanate.compose.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
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
    fun widthPx(): Float = LocalWindowInfo.current.containerSize.width.toFloat()

    /**
     * The height of the screen in pixels.
     */
    @Composable
    fun heightPx(): Float = LocalWindowInfo.current.containerSize.height.toFloat()

    /**
     * The width of the screen in Dp.
     */
    @Composable
    fun widthDp(): Dp = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() }

    /**
     * The height of the screen in Dp.
     */
    @Composable
    fun heightDp(): Dp = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() }
}

/**
 * Screen utilities of Compose, for example `EmaUtils.Screen.Compose.widthDp()`.
 */
val EmaScreenUtils.Compose: EmaComposeScreenUtils
    get() = EmaComposeScreenUtils
