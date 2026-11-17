package com.carmabs.emanate.compose.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalInspectionMode
import com.carmabs.emanate.core.utils.EmaPreviewUtils

/**
 * Utilities of the Compose previews. Use them through `EmaUtils.Preview.Compose`.
 *
 * @author <a href=“mailto:apps.carmabs@gmail.com”>Carlos Mateo Benito</a>
 */
object EmaComposePreviewUtils {

    /**
     * True inside a preview of the IDE.
     */
    @Composable
    fun isInPreview(): Boolean = LocalInspectionMode.current

    /**
     * Draws [skipContent], or [previewComposable] inside a preview, for example to skip content that can't run there.
     */
    @Composable
    fun skipForPreview(previewComposable: (@Composable () -> Unit)? = null, skipContent: @Composable () -> Unit) {
        if (!isInPreview()) {
            skipContent.invoke()
        } else {
            previewComposable?.invoke()
        }
    }
}

/**
 * Utilities of the Compose previews, for example `EmaUtils.Preview.Compose.isInPreview()`.
 */
val EmaPreviewUtils.Compose: EmaComposePreviewUtils
    get() = EmaComposePreviewUtils
