@file:Suppress("DEPRECATION")

package com.carmabs.emanate.android.extension

import android.annotation.SuppressLint
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.activity.OnBackPressedCallback
import androidx.core.os.BuildCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import com.carmabs.emanate.android.constants.EMA_RESULT_KEY
import com.carmabs.emanate.android.navigation.EmaNavigationBackHandler
import com.carmabs.emanate.core.model.EmaBackHandlerStrategy
import com.google.gson.Gson

/*
 * Extensions for fragment
 *
 * @author <a href=“mailto:apps.carmabs@gmail.com”>Carlos Mateo</a>
 */

/**
 * Add a listener to handle the hardware back button.
 * @param listener Listener to handle the back event. It must return true to enable default back behaviour. False to enable custom
 * back behaviour.
 */
@SuppressLint("UnsafeOptInUsageError")
fun Fragment.addOnBackPressedListener(listener: () -> EmaBackHandlerStrategy): EmaNavigationBackHandler =
    requireActivity().addOnBackPressedListener(viewLifecycleOwner, listener)

inline fun <reified T> Fragment.setEmaResultListener(crossinline listener: (result: T) -> Unit) {
    setFragmentResultListener(EMA_RESULT_KEY) { key, bundle ->
        val dataJson = bundle.getString(EMA_RESULT_KEY)
        val data = kotlin.runCatching {
            dataJson?.let {
                Gson().fromJson(it, T::class.java)
            }
        }.getOrNull()
        data?.also(listener)
    }
}
