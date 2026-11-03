@file:Suppress("DEPRECATION")

package com.carmabs.emanate.android.ui

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.viewbinding.ViewBinding
import com.carmabs.emanate.android.delegates.emaStateDelegate

/**
 *
 * Abstract base class to implement custom layouts.
 *
 * @author <a href=“mailto:apps.carmabs@gmail.com”>Carlos Mateo</a>
 */
@Deprecated(
    "Emanate will work only with Compose and EmaViewModel. Build the screens with emanate-compose-android and EmaViewModel instead of Views."
)
abstract class EmaLayout<B : ViewBinding, T : Any> : FrameLayout {

    var binding: B? = null
        private set

    var viewsSetup = false
        private set

    var data: T by emaStateDelegate {
        createInitialState()
    }

    protected abstract fun createInitialState(): T

    fun updateData(updateAction: T.() -> T): T {
        data = data.let(updateAction)
        binding?.also {
            it.setup(data)
        }
        return data
    }

    constructor(context: Context) : super(context) {
        onCreateView(context)
    }

    constructor(ctx: Context, attrs: AttributeSet) : super(ctx, attrs) {
        onCreateView(context, attrs)
    }

    constructor(ctx: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        ctx,
        attrs,
        defStyleAttr
    ) {
        onCreateView(context, attrs)
    }

    private fun onCreateView(context: Context, attrs: AttributeSet? = null) {
        viewsSetup = false
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        createViewBinding(inflater, this).also {
            binding = it
            addView(it.root)
        }
        handleAttributes(attrs)
    }

    /**
     * Setup called once the windows has been attached
     */
    final override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        onViewCreated()
        binding?.let {
            it.setup(data)
            viewsSetup = true
        }
    }

    /**
     * Called once the view has been created
     */
    protected open fun onViewCreated() {
        // IMPLEMENT BY CHILD CLASSES IF IT IS NECESSARY
    }

    /**
     * Method called once the custom attributes has been set by [EmaLayout.getAttributes]
     * @param ta are the custom attributes inflated
     */
    abstract fun setupAttributes(ta: TypedArray)

    /**
     * @return the custom attributes than can be used for the custom layout
     */
    abstract fun getAttributes(): IntArray?

    /**
     * Method called once the layout has been inflated implementing the methods [EmaLayout.getLayout]
     * @param mainLayout is the layout inflated instance
     */
    abstract fun B.setup(data: T)

    /**
     * @return the layout of the fragment to be inflated in the [EmaLayout.onCreateView]
     */
    abstract fun createViewBinding(inflater: LayoutInflater, container: ViewGroup?): B

    /**
     * Handle the custom attributes of the view
     */
    private fun handleAttributes(set: AttributeSet?) {
        set?.let { _ ->
            val attrs = getAttributes()
            attrs?.let { _ ->
                val ta = context.obtainStyledAttributes(set, attrs, 0, 0)
                try {
                    setupAttributes(ta)
                } finally {
                    ta.recycle()
                }
            }
        }
    }
}
