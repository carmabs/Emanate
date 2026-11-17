package com.carmabs.emanate.compose.extension

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.filter

/**
 * Created by Carlos Mateo Benito on 3/9/23.
 *
 * <p>
 * Copyright (c) 2023 by Carmabs. All rights reserved.
 * </p>
 *
 * @author <a href=“mailto:apps.carmabs@gmail.com”>Carlos Mateo Benito</a>
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedVisibilityScope.setOnVisibleListener(listener: suspend () -> Unit) {
    LaunchedEffect(key1 = Unit) {
        snapshotFlow { transition.currentState }
            .filter { it == EnterExitState.Visible }
            .collect {
                listener()
            }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedVisibilityScope.setOnBeforeVisibleListener(listener: suspend () -> Unit) {
    LaunchedEffect(key1 = Unit) {
        snapshotFlow { transition.currentState }
            .filter { it == EnterExitState.PreEnter }
            .collect {
                listener()
            }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedVisibilityScope.setOnHideListener(listener: suspend () -> Unit) {
    LaunchedEffect(key1 = Unit) {
        snapshotFlow { transition.targetState }
            .filter { it == EnterExitState.PostExit }
            .collect {
                listener()
            }
    }
}
