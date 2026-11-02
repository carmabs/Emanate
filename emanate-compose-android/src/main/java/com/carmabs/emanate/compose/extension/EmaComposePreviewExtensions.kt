package com.carmabs.emanate.compose.extension

import androidx.compose.runtime.Composable
import com.carmabs.emanate.compose.utils.Compose
import com.carmabs.emanate.core.utils.EmaUtils

/*
 * Created by Carlos Mateo Benito on 13/5/23
 *
 * <p>
 * Copyright (c) 2023 by Carmabs. All rights reserved.
 * </p>
 *
 * @author <a href=“mailto:apps.carmabs@gmail.com”>Carlos Mateo Benito</a>
 */
/**
 * Returns [value] inside a preview of the IDE, and this value otherwise.
 */
@Composable
fun <T> T?.changeForPreview(value: T?) = if (EmaUtils.Preview.Compose.isInPreview()) {
    value
} else {
    this
}
