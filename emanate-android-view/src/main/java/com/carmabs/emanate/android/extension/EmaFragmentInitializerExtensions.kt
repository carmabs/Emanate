@file:Suppress("DEPRECATION")

package com.carmabs.emanate.android.extension

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.carmabs.emanate.android.initializer.bundle.strategy.BundleSerializerStrategy
import com.carmabs.emanate.core.initializer.EmaInitializer

inline fun <reified I : EmaInitializer> Fragment.setInitializer(initializer: I, strategy: BundleSerializerStrategy) {
    val bundle = Bundle()
    bundle.setInitializer(initializer, strategy)
    arguments = bundle
}

/**
 * Get the incoming initializer from another fragment/activity
 */
inline fun <reified I : EmaInitializer> Fragment.getInitializer(serializerStrategy: BundleSerializerStrategy): I? =
    arguments?.getInitializer(serializerStrategy)
