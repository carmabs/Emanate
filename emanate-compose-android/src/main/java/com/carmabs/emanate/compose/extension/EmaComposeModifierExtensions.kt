package com.carmabs.emanate.compose.extension

/*
 * Created by Carlos Mateo Benito on 31/7/23.
 *
 * <p>
 * Copyright (c) 2023 by Carmabs. All rights reserved.
 * </p>
 *
 * @author <a href=“mailto:apps.carmabs@gmail.com”>Carlos Mateo Benito</a>
 */

import androidx.annotation.FloatRange
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import com.carmabs.emanate.core.constants.FLOAT_ONE
import com.carmabs.emanate.core.constants.FLOAT_ZERO

@Composable
fun Modifier.fade(
    fadeSide: FadeSide = FadeSide.BOTTOM,
    @FloatRange(
        FLOAT_ZERO.toDouble(),
        FLOAT_ONE.toDouble(),
        true,
        true
    ) startFadePercentage: Float = fadeSide.startFadingPercentage
): Modifier = graphicsLayer {
    compositingStrategy = CompositingStrategy.Offscreen
}.drawWithContent {
    drawContent()
    val brush = when (fadeSide) {
        FadeSide.TOP -> Brush.verticalGradient(
            FLOAT_ZERO to Color.Transparent,
            startFadePercentage to Color.Black,
            FLOAT_ONE to Color.Black
        )

        FadeSide.START -> Brush.horizontalGradient(
            FLOAT_ZERO to Color.Transparent,
            startFadePercentage to Color.Black,
            FLOAT_ONE to Color.Black
        )

        FadeSide.END -> Brush.horizontalGradient(
            FLOAT_ZERO to Color.Black,
            startFadePercentage to Color.Black,
            FLOAT_ONE to Color.Transparent
        )

        FadeSide.BOTTOM -> Brush.verticalGradient(
            FLOAT_ZERO to Color.Black,
            startFadePercentage to Color.Black,
            FLOAT_ONE to Color.Transparent
        )
    }
    drawRect(
        brush = brush,
        blendMode = BlendMode.DstIn
    )
}

enum class FadeSide(internal val startFadingPercentage: Float) {
    TOP(0.2f),
    START(0.2f),
    END(0.8f),
    BOTTOM(0.8f)
}
