package com.carmabs.emanate.ui.dialog.simple

import com.carmabs.emanate.core.model.EmaText

/**
 * What a simple dialog shows: a title, a message and, optionally, an image above them. An error dialog
 * draws the image with the colors of the errors.
 */
data class SimpleDialogData(
    val title: EmaText,
    val message: EmaText,
    val image: Int? = null,
    val isError: Boolean = false
)
