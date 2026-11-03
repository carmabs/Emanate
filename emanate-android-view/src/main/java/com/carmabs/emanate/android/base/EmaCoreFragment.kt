@file:Suppress("DEPRECATION")

package com.carmabs.emanate.android.base

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.carmabs.emanate.android.constants.EMA_RESULT_CODE
import com.carmabs.emanate.android.constants.EMA_RESULT_KEY
import com.carmabs.emanate.android.extension.addOnBackPressedListener
import com.carmabs.emanate.android.extension.generateViewModel
import com.carmabs.emanate.android.initializer.bundle.BundleSerializer
import com.carmabs.emanate.android.initializer.bundle.strategy.BundleSerializerStrategy
import com.carmabs.emanate.android.navigation.EmaActivityBackDelegate
import com.carmabs.emanate.android.ui.EmaAndroidView
import com.carmabs.emanate.core.constants.INT_ZERO
import com.carmabs.emanate.core.initializer.EmaInitializerSerializer
import com.carmabs.emanate.core.model.EmaBackHandlerStrategy
import com.carmabs.emanate.core.navigator.EmaNavigator
import com.carmabs.emanate.core.savestate.EmaStateStore
import com.carmabs.emanate.core.state.EmaEvent
import com.carmabs.emanate.core.state.EmaState
import com.carmabs.emanate.core.view.EmaViewModelTrigger
import com.carmabs.emanate.core.viewmodel.EmaViewModelContract
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

/**
 *
 * Abstract base class to implement Fragment with binding to ViewModel
 *
 * @author <a href=“mailto:apps.carmabs@gmail.com”>Carlos Mateo</a>
 */
@Deprecated(
    "Emanate will work only with Compose and EmaViewModel. Build the screens with emanate-compose-android and EmaViewModel instead of Views."
)
abstract class EmaCoreFragment<S : EmaState, VM : EmaViewModelContract<S, E>, E : EmaEvent> :
    Fragment(),
    EmaAndroidView<S, VM, E> {

    open val handleBackPressedManually = false

    val viewScope: CoroutineScope
        get() = viewLifecycleOwner.lifecycleScope

    private var viewJob: MutableList<Job>? = null

    @Suppress("UNCHECKED_CAST")
    override val viewModel: VM by lazy {
        generateViewModel(::provideViewModel).emaViewModel as VM
    }

    /**
     * Trigger to start viewmodel only when startViewModel is launched
     */
    override val startTrigger: EmaViewModelTrigger? = null

    protected open fun provideToolbarTitle(): String? = null

    /**
     * Previous state for comparing state properties update
     */
    final override var previousState: S? = null

    abstract override val navigator: EmaNavigator<E>?

    /**
     * Creates the view model of the screen.
     *
     * @param store saves the values of the backup of the view model in the SavedStateHandle of the screen
     */
    abstract fun provideViewModel(store: EmaStateStore): VM
    final override val initializerSerializer: EmaInitializerSerializer?
        get() = arguments?.let { bundle ->
            BundleSerializer(bundle, initializerStrategy)
        }

    abstract val initializerStrategy: BundleSerializerStrategy
    override val coroutineScope: CoroutineScope
        get() = lifecycleScope

    /**
     * Get the scope of the fragment depending the viewModelScopeSelected
     */
    protected fun getScope(): CoroutineScope = if (fragmentViewModelScope) {
        coroutineScope
    } else {
        requireActivity().lifecycleScope
    }

    @CallSuper
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        previousState = null
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!handleBackPressedManually) {
            addOnBackPressedListener {
                val parentActivity = requireActivity()
                if (parentActivity is EmaActivityBackDelegate) {
                    if (parentActivity.ownsBackDelegate) {
                        parentActivity.onBackDelegate()
                    } else {
                        navigateBack()
                        // Cancel because we are handling manually the navigation with navigateBack()
                        EmaBackHandlerStrategy.Cancelled
                    }
                } else {
                    navigateBack()
                    // Cancel because we are handling manually the navigation with navigateBack()
                    EmaBackHandlerStrategy.Cancelled
                }
            }
        }

        onCreate(viewModel)
    }

    /**
     * The view model is instantiated on fragment resume.
     */
    @CallSuper
    override fun onStart() {
        super.onStart()
        onStartView(viewModel)
    }

    /**
     * Notifies the view model that view has been gone to foreground.
     */
    @CallSuper
    override fun onResume() {
        // It will set the last value of normalContentData, and avoid to delivered the last value of flow if it has not be updated due to
        // updateToDataState
        // We call this and not in onStart, because is some cases, the view that receives the state could be use a
        // dialog, that needs to be restored after onRestoreInstance state is called. Otherwise, java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState
        // could be launched.
        // On restoreInstanceState is called between onStart and onResume, on re-initialization, so binding the views here, guarantees the state of
        // savedInstances has been restored
        if (viewJob == null) {
            viewJob = onBindView(getScope(), viewModel)
        }
        super.onResume()
        onResumeView(viewModel)
    }

    /**
     * Notifies the view model that view has been gone to background.
     */
    @CallSuper
    override fun onPause() {
        super.onPause()
        onPauseView(viewModel)
    }

    /**
     * Determine if the view model lifecycle is attached to the Activity or to the Fragment
     */
    open val fragmentViewModelScope: Boolean = true

    @CallSuper
    override fun onStop() {
        super.onStop()
        onUnbindView(viewJob, viewModel)
        viewJob = null
    }

    @CallSuper
    override fun onDestroyView() {
        previousState = null
        super.onDestroyView()
    }

    final override fun onBack(result: Any?): Boolean {
        val gson = Gson()
        val hasMoreFragments = kotlin.runCatching {
            findNavController().popBackStack()
        }.getOrNull() ?: let {
            val hasMoreFragments = parentFragmentManager.backStackEntryCount > INT_ZERO
            result?.also {
                setFragmentResult(
                    EMA_RESULT_KEY,
                    Bundle().apply {
                        putString(EMA_RESULT_KEY, gson.toJson(it))
                    }
                )
            }
            if (hasMoreFragments) {
                parentFragmentManager.popBackStack()
            }
            hasMoreFragments
        }

        if (!hasMoreFragments) {
            result?.also {
                requireActivity().setResult(
                    EMA_RESULT_CODE,
                    Intent().putExtra(EMA_RESULT_KEY, gson.toJson(it))
                )
            }
            requireActivity().finish()
        }
        return hasMoreFragments
    }
}
