@file:Suppress("DEPRECATION")

package com.carmabs.emanate.android.ui

import android.animation.Animator
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.viewbinding.ViewBinding
import com.carmabs.emanate.android.initializer.bundle.strategy.BundleSerializerStrategy
import com.carmabs.emanate.android.navigation.EmaNavControllerNavigator
import com.carmabs.emanate.android.utils.Android
import com.carmabs.emanate.core.constants.FLOAT_ONE
import com.carmabs.emanate.core.constants.FLOAT_ZERO
import com.carmabs.emanate.core.state.EmaEvent
import com.carmabs.emanate.core.state.EmaState
import com.carmabs.emanate.core.utils.EmaUtils
import com.carmabs.emanate.core.viewmodel.EmaViewModelContract
import com.google.android.material.appbar.AppBarLayout

/**
 *
 * Base activity to bind and unbind view model
 *
 * @author <a href=“mailto:apps.carmabs@gmail.com”>Carlos Mateo</a>
 */
@Deprecated(
    "Emanate will work only with Compose and EmaViewModel. Build the screens with emanate-compose-android and EmaViewModel instead of Views."
)
abstract class EmaToolbarActivity<B : ViewBinding, S : EmaState, VM : EmaViewModelContract<S, E>, E : EmaEvent> :
    EmaActivity<B, S, VM, E>() {

    override val initializerStrategy: BundleSerializerStrategy
        get() = BundleSerializerStrategy.EMPTY

    /**
     * Title for toolbar. If it is null the label xml tag in navigation layout is set for the toolbar
     * title, otherwise this title will be set for all fragments inside this activity.
     */
    protected open fun provideFixedToolbarTitle(): String? = null

    /**
     * Setup the toolbar
     * @param savedInstanceState for activity recreation
     */
    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        setupActionBars()
    }

    /**
     * The toobar for the activity
     */
    protected val toolbar: Toolbar by lazy {
        binding.provideToolbar()
    }

    /**
     * The toolbar container for the activity
     */
    protected val toolbarLayout: AppBarLayout by lazy {
        binding.provideToolbarLayout()
    }

    /**
     * Request the action bar setup
     */
    private fun setupActionBars() {
        setupToolbar(toolbar)
    }

    abstract fun B.provideToolbar(): Toolbar

    abstract fun B.provideToolbarLayout(): AppBarLayout

    /**
     * Find the toolbar and its container for the activity. The toolbar must have the
     * id=@+id/emaToolbar. The toolbar contaienr [AppBarLayout] must have the id=@+ìd/emaAppBarLayout
     */
    private fun setupToolbar(toolbar: Toolbar) {
        setSupportActionBar(toolbar)
        (navigator as? EmaNavControllerNavigator<*>)?.navController?.also {
            setupActionBarWithNavController(it)
            it.addOnDestinationChangedListener { _, destination, _ ->
                setToolbarTitle(provideFixedToolbarTitle())
            }
        }
    }

    protected fun setToolbarTitle(title: String?) {
        supportActionBar?.title =
            title ?: provideFixedToolbarTitle()
                ?: (navigator as? EmaNavControllerNavigator<*>)?.navController?.currentDestination?.label
    }

    /**
     * Hides the toolbar
     */
    protected fun hideToolbar(gone: Boolean = true, animate: Boolean = true) {
        if (animate) {
            toolbarLayout.animate().apply {
                alpha(FLOAT_ZERO)
                setListener(object : Animator.AnimatorListener {

                    override fun onAnimationEnd(p0: Animator) {
                        toolbarLayout.visibility = EmaUtils.View.Android.visibility(false, gone)
                    }

                    override fun onAnimationStart(p0: Animator) {
                    }

                    override fun onAnimationRepeat(p0: Animator) {
                    }

                    override fun onAnimationCancel(p0: Animator) {
                    }
                })
            }
        } else {
            toolbarLayout.visibility = if (gone) View.GONE else View.INVISIBLE
        }
    }

    /**
     * Show the toolbar
     */
    protected fun showToolbar(animate: Boolean = true) {
        if (animate) {
            toolbarLayout.animate().apply {
                alpha(FLOAT_ONE)
                setListener(object : Animator.AnimatorListener {

                    override fun onAnimationStart(p0: Animator) {
                        toolbarLayout.visibility = View.VISIBLE
                    }

                    override fun onAnimationEnd(p0: Animator) {
                    }

                    override fun onAnimationCancel(p0: Animator) {
                    }

                    override fun onAnimationRepeat(p0: Animator) {
                    }
                })
            }
        } else {
            toolbarLayout.visibility = View.VISIBLE
        }
    }
}
