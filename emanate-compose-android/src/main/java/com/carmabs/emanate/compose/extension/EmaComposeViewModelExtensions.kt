@file:Suppress("UNCHECKED_CAST")

package com.carmabs.emanate.compose.extension

import com.carmabs.emanate.core.action.EmaAction
import com.carmabs.emanate.core.action.EmaActionDispatcher
import com.carmabs.emanate.core.viewmodel.EmaViewModelContract

/**
 * Created by Carlos Mateo Benito on 12/9/23.
 *
 * <p>
 * Copyright (c) 2023 by Carmabs. All rights reserved.
 * </p>
 *
 * @author <a href=“mailto:apps.carmabs@gmail.com”>Carlos Mateo Benito</a>
 */
fun <A : EmaAction> EmaViewModelContract<*, *>.asActionDispatcher(): EmaActionDispatcher<A> =
    (this as? EmaActionDispatcher<A>)
        ?: throw IllegalStateException("${this::class} must implement EmaActionDispatcher with the proper action")
