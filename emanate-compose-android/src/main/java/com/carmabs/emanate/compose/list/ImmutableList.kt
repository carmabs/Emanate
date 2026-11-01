package com.carmabs.emanate.compose.list

import androidx.compose.runtime.Immutable

/**
 * Created by Carlos Mateo Benito on 15/9/23.
 *
 * <p>
 * Copyright (c) 2023 by Carmabs. All rights reserved.
 * </p>
 *
 * @author <a href=“mailto:apps.carmabs@gmail.com”>Carlos Mateo Benito</a>
 */
@Immutable
class EmaImmutableList<T>(val list: List<T>) : List<T> by list

fun <T> List<T>.toImmutable() = EmaImmutableList(this)
