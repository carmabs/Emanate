@file:Suppress("DEPRECATION")

package com.carmabs.emanate.android.ui.recycler

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
abstract class EmaMultiRecyclerAdapter<I : Any>(diffCallback: DiffUtil.ItemCallback<I> = getDefaultDiffCallback()) :
    EmaBaseRecyclerAdapter<I>(diffCallback) {

    /**
     * Method called to set the ViewHolder. If different view holders are provided depending [viewType], use enableMultiViewHolder function
     * @param parent holder where view components are implemented
     * @param viewType of the item
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmaViewHolder<I> =
        createMultiViewHolder(parent, viewType)

    /**
     * Method to set the view parameters from each item
     */
    protected abstract fun ViewBinding.bind(
        item: I,
        viewType: Int,
        holder: EmaViewHolder<I>,
        payloads: MutableList<Any>
    )

    /**
     * Function to implement different viewHolders depending the viewType provided.
     */
    protected abstract fun createMultiViewHolder(view: ViewGroup, viewType: Int): EmaAdapterMultiViewHolder

    protected open inner class EmaAdapterMultiViewHolder(
        private val viewBinding: ViewBinding,
        private val viewType: Int
    ) : EmaViewHolder<I>(viewBinding.root) {
        override fun bind(item: I, holder: EmaViewHolder<I>, payloads: MutableList<Any>) {
            viewBinding.bind(item, viewType, holder, payloads)
            itemView.setOnClickListener {
                onItemClicked(item, adapterPosition, itemCount)
                itemClickListener?.invoke(adapterPosition, item)
            }
        }
    }
}
