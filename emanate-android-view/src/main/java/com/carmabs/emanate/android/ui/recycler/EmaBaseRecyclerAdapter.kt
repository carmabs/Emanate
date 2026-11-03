@file:Suppress("DEPRECATION")

package com.carmabs.emanate.android.ui.recycler

import android.annotation.SuppressLint
import androidx.annotation.CallSuper
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import java.util.*

/**
 * Adapter to implement the list view interface in recycler views
 *

 *
 * @author <a href="mailto:apps.carmabs@gmail.com">Carlos Mateo Benito</a>
 */

@Deprecated(
    "Emanate will work only with Compose and EmaViewModel. Build the screens with emanate-compose-android and EmaViewModel instead of Views."
)
abstract class EmaBaseRecyclerAdapter<I : Any>(diffCallback: DiffUtil.ItemCallback<I> = getDefaultDiffCallback()) :
    ListAdapter<I, EmaViewHolder<I>>(diffCallback) {

    /**
     * Listener when item is clicked
     */
    protected var itemClickListener: ((index: Int, item: I) -> Unit)? = null

    /**
     * Method called to implement the view data set up
     * @param holder holder where view components are implemented
     * @param position current item position in recycler view
     */
    final override fun onBindViewHolder(holder: EmaViewHolder<I>, position: Int) {}

    override fun onBindViewHolder(holder: EmaViewHolder<I>, position: Int, payloads: MutableList<Any>) {
        super.onBindViewHolder(holder, position, payloads)
        holder.bind(getItem(position), holder, payloads)
    }

    /**
     * Update the adapter with a new list
     * @param listUpdate to set in the adapter
     */
    @CallSuper
    open fun updateList(listUpdate: List<I>) {
        submitList(listUpdate)
    }

    /**
     * Update the item of the list if exists. It must have the same memory reference
     * @param item to update
     */
    @CallSuper
    open fun updateItem(item: I) {
        val index = currentList.indexOf(item)
        if (index > -1) {
            submitList(currentList.toMutableList().apply { set(index, item) })
        }
    }

    /**
     * Add listener when item is clicked
     * @param listener for click on view item
     */
    fun setOnItemClickListener(itemClickListener: ((index: Int, item: I) -> Unit)?) {
        this.itemClickListener = itemClickListener
    }

    /**
     * Add item to the list
     * @param item to add
     */
    @CallSuper
    open fun addItem(item: I, position: Int = itemCount) {
        submitList(currentList.toMutableList().apply { add(position, item) })
    }

    /**
     * Remove item from the list
     * @param position to remove
     */
    @CallSuper
    open fun removeItem(position: Int) {
        submitList(currentList.toMutableList().apply { removeAt(position) })
    }

    protected open fun onItemClicked(item: I, position: Int, size: Int) {
        // Override to add internal logic when item is clicked
    }

    companion object {
        fun <I : Any> getDefaultDiffCallback() = object : DiffUtil.ItemCallback<I>() {
            override fun areItemsTheSame(oldItem: I, newItem: I): Boolean = oldItem == newItem

            @SuppressLint("DiffUtilEquals")
            override fun areContentsTheSame(oldItem: I, newItem: I): Boolean = (oldItem == newItem)
        }

        fun <I : Any> getAlwaysUpdateCallback() = object : DiffUtil.ItemCallback<I>() {
            override fun areItemsTheSame(oldItem: I, newItem: I): Boolean = false

            @SuppressLint("DiffUtilEquals")
            override fun areContentsTheSame(oldItem: I, newItem: I): Boolean = false
        }
    }
}
