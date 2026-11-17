package com.carmabs.emanate.compose.savestate

import androidx.lifecycle.SavedStateHandle
import com.carmabs.emanate.core.savestate.EmaStateStore

/**
 * Created by Carlos Mateo Benito on 13/10/2026.
 *
 * Store of the values saved by an EmaViewModel in the [SavedStateHandle] of its screen. The keys have a prefix, so they
 * don't clash with the other values of the handle, like the navigation arguments.
 *
 * <p>
 * Copyright (c) 2026 by Carmabs. All rights reserved.
 * </p>
 *
 * @author <a href=“mailto:apps.carmabs@gmail.com”>Carlos Mateo Benito</a>
 */
internal class SavedStateHandleStore(private val savedStateHandle: SavedStateHandle) : EmaStateStore {

    override fun read(key: String): String? = savedStateHandle[KEY_PREFIX + key]

    override fun write(key: String, value: String?) {
        if (value == null) {
            savedStateHandle.remove<String>(KEY_PREFIX + key)
        } else {
            savedStateHandle[KEY_PREFIX + key] = value
        }
    }

    internal companion object {
        const val KEY_PREFIX = "emanate.saved."
    }
}
