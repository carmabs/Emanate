package com.carmabs.emanate.compose.extension

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import com.carmabs.emanate.core.model.EmaText

/*
 * Created by Carlos Mateo Benito on 25/12/21.
 *
 * <p>
 * Copyright (c) 2021 by Carmabs. All rights reserved.
 * </p>
 *
 * @author <a href=“mailto:apps.carmabs@gmail.com”>Carlos Mateo Benito</a>
 */

/**
 * Arguments to format the text. The factories of [EmaText] always provide an array, so an empty one means that the
 * text has no arguments and must not be formatted, like a text that contains a % sign.
 */
private val EmaText.formatArgs: Array<out Any>?
    get() = data?.takeIf { it.isNotEmpty() }

/**
 * Transform ema text to string value.
 */
@Composable
fun EmaText.stringResource(): String = when (this) {
    is EmaText.Id -> formatArgs?.let { androidx.compose.ui.res.stringResource(id, *it) }
        ?: androidx.compose.ui.res.stringResource(id)

    is EmaText.Plural -> {
        formatArgs?.let {
            pluralStringResource(id, quantity, *it)
        } ?: pluralStringResource(
            id,
            quantity
        )
    }

    is EmaText.Text -> formatArgs?.let { String.format(text, *it) } ?: text

    is EmaText.Composition -> texts.map { it.stringResource() }.reduce { acc, string ->
        acc + string
    }
}
