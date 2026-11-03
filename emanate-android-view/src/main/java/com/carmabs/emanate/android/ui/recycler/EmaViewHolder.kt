@file:Suppress("DEPRECATION")

package com.carmabs.emanate.android.ui.recycler

import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * View holder class for item representation in recycler view.
 * Extend this ViewHolder and use bind to implement the holder with the item data
 *

 *
 * @author <a href="mailto:apps.carmabs@gmail.com">Carlos Mateo Benito</a>
 */

@Deprecated(
    "Emanate will work only with Compose and EmaViewModel. Build the screens with emanate-compose-android and EmaViewModel instead of Views."
)
abstract class EmaViewHolder<I>(view: View) : RecyclerView.ViewHolder(view) {

    /**
     * Methods called to implement the item view components
     * @param item shown in recycler view
     */
    abstract fun bind(item: I, holder: EmaViewHolder<I>, payloads: MutableList<Any>)
}
