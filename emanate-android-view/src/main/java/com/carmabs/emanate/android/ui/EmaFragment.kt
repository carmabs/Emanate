@file:Suppress("DEPRECATION")

package com.carmabs.emanate.android.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.viewbinding.ViewBinding
import com.carmabs.emanate.android.base.EmaCoreFragment
import com.carmabs.emanate.android.initializer.bundle.strategy.BundleSerializerStrategy
import com.carmabs.emanate.core.state.EmaEvent
import com.carmabs.emanate.core.state.EmaState
import com.carmabs.emanate.core.viewmodel.EmaViewModelContract

/**
 *
 * Abstract base class to implement ViewBinding in fragment
 *
 * @author <a href=“mailto:apps.carmabs@gmail.com”>Carlos Mateo</a>
 */
@Deprecated(
    "Emanate will work only with Compose and EmaViewModel. Build the screens with emanate-compose-android and EmaViewModel instead of Views."
)
abstract class EmaFragment<B : ViewBinding, S : EmaState, VM : EmaViewModelContract<S, E>, E : EmaEvent> :
    EmaCoreFragment<S, VM, E>() {

    override val initializerStrategy: BundleSerializerStrategy
        get() = BundleSerializerStrategy.EMPTY
    protected var isFirstNormalExecution: Boolean = true
        private set

    private var _binding: B? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    protected val binding
        get() = _binding!!

    /**
     * Method to provide the fragment ViewBinding class to represent the layout.
     */
    abstract fun createViewBinding(inflater: LayoutInflater, container: ViewGroup?): B

    @CallSuper
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        isFirstNormalExecution = true
        _binding = createViewBinding(inflater, container)
        return binding.root
    }

    @CallSuper
    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    final override fun onState(state: S) {
        binding.onState(state)
        isFirstNormalExecution = false
    }

    final override suspend fun onEvent(event: E) {
        binding.onEvent(event)
    }

    abstract fun B.onState(state: S)
    protected open suspend fun B.onEvent(event: E) = Unit
}
