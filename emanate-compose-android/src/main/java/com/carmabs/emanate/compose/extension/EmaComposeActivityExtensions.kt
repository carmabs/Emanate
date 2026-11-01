package com.carmabs.emanate.compose.extension

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalContext
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Created by Carlos Mateo Benito on 12/5/23
 *
 * <p>
 * Copyright (c) 2023 by Carmabs. All rights reserved.
 * </p>
 *
 * @author <a href=“mailto:apps.carmabs@gmail.com”>Carlos Mateo Benito</a>
 */

val ProvidableCompositionLocal<Context>.activity: ComponentActivity
    @Composable get() {
        val context = current
        val activity = remember {
            var contextSearch = context
            while (contextSearch is ContextWrapper) {
                if (contextSearch is ComponentActivity) {
                    return@remember contextSearch
                }
                contextSearch = contextSearch.baseContext
            }
            throw IllegalStateException("No component Activity container found for this composable")
        }
        return activity
    }
