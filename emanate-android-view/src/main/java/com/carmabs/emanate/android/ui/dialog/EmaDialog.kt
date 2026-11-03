@file:Suppress("DEPRECATION")

package com.carmabs.emanate.android.ui.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.annotation.CallSuper
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.viewbinding.ViewBinding
import com.carmabs.emanate.android.delegates.emaStateDelegate
import com.carmabs.emanate.android.extension.getSerializableCompat
import com.carmabs.emanate.android.utils.Android
import com.carmabs.emanate.core.dialog.EmaDialogData
import com.carmabs.emanate.core.dialog.EmaDialogListener
import com.carmabs.emanate.core.utils.EmaUtils

/**
 * Base dialog class to implement dialogs
 *
 * @author <a href="mailto:apps.carmabs@gmail.com">Carlos Mateo Benito</a>
 */
@Deprecated(
    "Emanate will work only with Compose and EmaViewModel. Build the screens with emanate-compose-android and EmaViewModel instead of Views."
)
abstract class EmaDialog<B : ViewBinding, T : EmaDialogData> :
    DialogFragment(),
    DialogInterface.OnShowListener {

    private var _binding: B? = null
    protected val binding
        get() = _binding!!

    companion object {
        private const val KEY_DIALOG_DATA = "KEY_DIALOG_DATA"
    }

    var dialogListener: EmaDialogListener? = null

    var data: T by emaStateDelegate {
        createInitialState()
    }
        private set

    private var isDismissed: Boolean = false

    protected open val disableBackButton
        get() = !isCancelable

    /**
     * Specify the ViewBinding to be inflated in the [EmaDialog.onCreateView].
     */
    abstract fun createViewBinding(inflater: LayoutInflater, container: ViewGroup?): B

    /**
     * Setup data for UI
     */
    protected abstract fun B.setup(data: T)

    @CallSuper
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnShowListener(this)
        dialog.setOnKeyListener { _, keyCode, event ->
            dialogListener?.let {
                if (!disableBackButton && keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_DOWN) {
                    dismissAllowingStateLoss()
                    it.onBackPressed()
                }
            }
            false
        }

        return dialog
    }

    fun updateData(updateAction: T.() -> T): T {
        data = data.let(updateAction)
        _binding?.setup(data)
        updateDialogSizeAndBehaviour()
        return data
    }

    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.setup(data)
    }

    @CallSuper
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = createViewBinding(inflater, container)
        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            requestFeature(Window.FEATURE_NO_TITLE)
        }
        return binding.root
    }

    @CallSuper
    override fun onResume() {
        super.onResume()
        updateDialogSizeAndBehaviour()
    }

    private fun updateDialogSizeAndBehaviour() {
        dialog?.window?.also { win ->
            val display = EmaUtils.Screen.Android.metrics(requireContext())
            val size = Point().apply {
                x = display.widthPixels
                y = display.heightPixels
            }
            data.run {
                val width = proportionWidth?.let { (it * size.x).toInt() }
                    ?: ViewGroup.LayoutParams.WRAP_CONTENT
                val height = proportionHeight?.let { (it * size.y).toInt() }
                    ?: ViewGroup.LayoutParams.WRAP_CONTENT
                win.setLayout(width, height)
                isCancelable = !isModal
                dialog?.setCanceledOnTouchOutside(isCancelable)
                dialog?.setOnCancelListener {
                    dialogListener?.onOutsidePressed()
                }
            }
        }
    }

    @CallSuper
    override fun onShow(p0: DialogInterface?) {
        if (isDismissed) {
            dismissAllowingStateLoss()
        }
    }

    protected abstract fun createInitialState(): T

    @CallSuper
    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    @CallSuper
    override fun onDestroy() {
        dialogListener?.onDestroyed()
        dialogListener = null
        super.onDestroy()
    }
}
