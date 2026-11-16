package com.carmabs.emanate.compose.extension

import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import com.carmabs.emanate.core.constants.INT_ZERO

/**
 * Created by Carlos Mateo Benito on 17/9/23
 *
 * <p>
 * Copyright (c) 2023 by Carmabs. All rights reserved.
 * </p>
 *
 * @author <a href=“mailto:apps.carmabs@gmail.com”>Carlos Mateo Benito</a>
 */
@Composable
fun EmaMeasureViewWidth(
    viewToMeasure: @Composable () -> Unit,
    content: @Composable (measuredWidth: Dp, viewMeasured: @Composable () -> Unit) -> Unit
) {
    SubcomposeLayout { constraints ->
        val measuredWidth = subcompose("viewToMeasure", viewToMeasure)[INT_ZERO]
            .measure(Constraints()).width.toDp()
        val contentPlaceable = subcompose("content") {
            content(measuredWidth, viewToMeasure)
        }[INT_ZERO].measure(constraints)
        layout(contentPlaceable.width, contentPlaceable.height) {
            contentPlaceable.place(INT_ZERO, INT_ZERO)
        }
    }
}

val Dp.inSp
    @Composable
    get() = run {
        val dp = this
        with(LocalDensity.current) { dp.toSp() }
    }
