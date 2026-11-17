package com.carmabs.emanate.compose.extension

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

/**
 * Created by Carlos Mateo Benito on 26/5/23
 *
 * <p>
 * Copyright (c) 2023 by Carmabs. All rights reserved.
 *
 * </p>
 *
 * @author <a href=“mailto:apps.carmabs@gmail.com”>Carlos Mateo Benito</a>
 */
@Composable
fun Int.pxToDp(): Dp {
    val density = LocalDensity.current
    return with(density) {
        toDp()
    }
}

@Composable
fun Float.pxToDp(): Dp {
    val density = LocalDensity.current
    return with(density) {
        toDp()
    }
}

@Composable
fun Dp.toPx(): Float = LocalDensity.current.run { toPx() }
