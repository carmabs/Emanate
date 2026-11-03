@file:Suppress("DEPRECATION")

package com.carmabs.emanate.android.ui.dialog

import android.util.Log
import androidx.fragment.app.FragmentManager
import com.carmabs.emanate.core.dialog.EmaDialogData
import com.carmabs.emanate.core.dialog.EmaDialogListener
import com.carmabs.emanate.core.dialog.EmaDialogProvider

/**
 * Simple dialog implementation
 *
 *
 * @author <a href="mailto:apps.carmabs@gmail.com">Carlos Mateo Benito</a>
 */
@Deprecated(
    "Emanate will work only with Compose and EmaViewModel. Build the screens with emanate-compose-android and EmaViewModel instead of Views."
)
abstract class EmaAndroidDialogProvider(
    private val fragmentManager: FragmentManager,
    private val dialogTag: String? = null
) : EmaDialogProvider {

    private val dialog: EmaDialog<*, EmaDialogData>?
        get() = fragmentManager.findFragmentByTag(tag) as? EmaDialog<*, EmaDialogData>

    abstract fun generateDialog(dialogData: EmaDialogData?): EmaDialog<*, *>

    override fun show(dialogData: EmaDialogData?) {
        // We use this to avoid fragment duplication due to internal fragment save state. It guarantees that on rotation
        // the same fragment is updated and not generate a new one
        dialog?.also {
            updateDialogData(it, dialogData)
        } ?: also {
            generateDialog(dialogData).also { dialog ->
                val emaDialog = dialog as EmaDialog<*, EmaDialogData>
                emaDialog.show(fragmentManager, tag)
                updateDialogData(emaDialog, dialogData)
            }
        }
    }

    private fun updateDialogData(dialog: EmaDialog<*, EmaDialogData>, dialogData: EmaDialogData?) {
        dialog.dialogListener = dialogListener
        dialogData?.also {
            dialog.updateData {
                it
            }
        }
    }

    override fun hide() {
        // It guarantees that fragment is totally destroyed when it is hidden. Otherwise, the fragment could be saved
        // internally and create duplications on fragment recreations.
        dialog?.let {
            if (!it.isHidden) {
                Log.d(tag, "Alternative dialog totally hidden")
                it.dismissAllowingStateLoss()
            }
            fragmentManager.beginTransaction().remove(it).commitNowAllowingStateLoss()
        }
    }

    override val isVisible: Boolean
        get() = fragmentManager.findFragmentByTag(tag)?.isVisible ?: false

    override var dialogListener: EmaDialogListener? = null
        set(value) {
            field = value
            dialog?.dialogListener = value
        }

    private val tag by lazy {
        dialogTag ?: javaClass.name.toString()
    }
}
