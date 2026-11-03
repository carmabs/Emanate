@file:Suppress("DEPRECATION")

package com.carmabs.emanate.android.ui.recycler

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.viewbinding.ViewBinding

/**
 * Adapter to implement the list view interface in recycler views
 *

 *
 * @author <a href="mailto:apps.carmabs@gmail.com">Carlos Mateo Benito</a>
 */

@Deprecated(
    "Emanate will work only with Compose and EmaViewModel. Build the screens with emanate-compose-android and EmaViewModel instead of Views."
)
abstract class EmaRecyclerAdapter<B : ViewBinding, I : Any>(
    diffCallback: DiffUtil.ItemCallback<I> = getDefaultDiffCallback()
) : EmaBaseRecyclerAdapter<I>(diffCallback) {

    /**
     * Method to provide the recycler item  ViewBinding class to represent the layout.
     */
    abstract fun createViewBinding(inflater: LayoutInflater, container: ViewGroup?): B

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmaViewHolder<I> = EmaAdapterViewHolder(
        createViewBinding(LayoutInflater.from(parent.context), parent),
        viewType
    )

    /**
     * Method to set the view parameters from each item
     */
    protected abstract fun B.bind(item: I, viewType: Int, holder: EmaViewHolder<I>, payloads: MutableList<Any>)

    protected open inner class EmaAdapterViewHolder(private val viewBinding: B, private val viewType: Int) :
        EmaViewHolder<I>(viewBinding.root) {

        override fun bind(item: I, holder: EmaViewHolder<I>, payloads: MutableList<Any>) {
            viewBinding.bind(item, viewType, holder, payloads)
            itemView.setOnClickListener {
                onItemClicked(item, adapterPosition, itemCount)
                itemClickListener?.invoke(adapterPosition, item)
            }
        }
    }
}
