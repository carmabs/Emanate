package com.carmabs.emanate.compose.savestate

import androidx.lifecycle.SavedStateHandle
import com.carmabs.emanate.core.state.EmaEvent
import com.carmabs.emanate.core.state.EmaState

/**
 * Created by Carlos Mateo Benito on 22/2/24.
 *
 * <p>
 * Copyright (c) 2024 by Carmabs. All rights reserved.
 * </p>
 *
 * @author <a href=“mailto:apps.carmabs@gmail.comm”>Carlos Mateo Benito</a>
 */
data class SavedStateSupport<S : EmaState, E : EmaEvent>(
    val savedStateHandle: SavedStateHandle,
    val saveStateManager: EmaSaveStateManager<S, E>
)
