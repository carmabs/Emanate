@file:Suppress("DEPRECATION")

package com.carmabs.emanate.android.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.carmabs.emanate.android.HostActivity
import com.carmabs.emanate.android.TextBinding
import com.carmabs.emanate.android.extension.clearAdapters
import com.carmabs.emanate.android.idleMain
import com.carmabs.emanate.android.ui.recycler.EmaBaseRecyclerAdapter
import com.carmabs.emanate.android.ui.recycler.EmaMultiRecyclerAdapter
import com.carmabs.emanate.android.ui.recycler.EmaRecyclerAdapter
import com.carmabs.emanate.android.ui.recycler.EmaSwipeToDeleteCallback
import com.carmabs.emanate.android.ui.recycler.EmaViewHolder
import com.carmabs.emanate.android.ui.recycler.decorator.EmaGridSpacingItemDecoration
import com.carmabs.emanate.android.ui.recycler.decorator.EmaVerticalSpaceDecoration
import com.carmabs.emanate.android.waitUntil
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

class TextAdapter : EmaRecyclerAdapter<TextBinding, String>() {
    val clicked = mutableListOf<Pair<String, Int>>()

    override fun createViewBinding(inflater: LayoutInflater, container: ViewGroup?) = TextBinding(inflater.context)

    override fun TextBinding.bind(
        item: String,
        viewType: Int,
        holder: EmaViewHolder<String>,
        payloads: MutableList<Any>
    ) {
        text.text = item
    }

    override fun onItemClicked(item: String, position: Int, size: Int) {
        clicked += item to size
    }
}

class DefaultClickAdapter : EmaRecyclerAdapter<TextBinding, String>(getAlwaysUpdateCallback()) {
    override fun createViewBinding(inflater: LayoutInflater, container: ViewGroup?) = TextBinding(inflater.context)
    override fun TextBinding.bind(
        item: String,
        viewType: Int,
        holder: EmaViewHolder<String>,
        payloads: MutableList<Any>
    ) {
        text.text = item
    }
}

class MultiAdapter : EmaMultiRecyclerAdapter<String>() {
    val clicked = mutableListOf<String>()

    override fun getItemViewType(position: Int) = position % 2

    override fun createMultiViewHolder(view: ViewGroup, viewType: Int) =
        EmaAdapterMultiViewHolder(TextBinding(view.context), viewType)

    override fun ViewBinding.bind(
        item: String,
        viewType: Int,
        holder: EmaViewHolder<String>,
        payloads: MutableList<Any>
    ) {
        (this as TextBinding).text.text = "$viewType:$item"
    }

    override fun onItemClicked(item: String, position: Int, size: Int) {
        clicked += item
    }
}

@GraphicsMode(GraphicsMode.Mode.NATIVE)
@RunWith(RobolectricTestRunner::class)
class EmaRecyclerTest {

    private val activity = Robolectric.buildActivity(HostActivity::class.java).setup().get()

    private fun recycler(
        adapter: RecyclerView.Adapter<*>,
        layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(activity)
    ) = RecyclerView(activity).apply {
        this.layoutManager = layoutManager
        this.adapter = adapter
        activity.setContentView(this)
    }

    private fun RecyclerView.layoutItems() {
        idleMain()
        measure(
            View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(2000, View.MeasureSpec.EXACTLY)
        )
        layout(0, 0, 1000, 2000)
    }

    private fun RecyclerView.texts() = (0 until childCount).map { (getChildAt(it) as TextView).text.toString() }

    @Test
    fun `the adapter binds the items and notifies the clicks`() {
        val adapter = TextAdapter()
        val recycler = recycler(adapter)
        val clicks = mutableListOf<Pair<Int, String>>()
        adapter.setOnItemClickListener { index, item -> clicks += index to item }

        adapter.updateList(listOf("a", "b", "c"))
        recycler.layoutItems()

        assertEquals(listOf("a", "b", "c"), recycler.texts())
        recycler.getChildAt(1).performClick()
        assertEquals(listOf(1 to "b"), clicks)
        assertEquals(listOf("b" to 3), adapter.clicked)
    }

    @Test
    fun `items can be added, removed and updated`() {
        val adapter = TextAdapter()
        val recycler = recycler(adapter)
        adapter.updateList(listOf("a", "b"))

        adapter.addItem("c")
        waitUntil { adapter.currentList == listOf("a", "b", "c") }

        adapter.addItem("z", position = 0)
        waitUntil { adapter.currentList == listOf("z", "a", "b", "c") }

        adapter.removeItem(0)
        waitUntil { adapter.currentList == listOf("a", "b", "c") }

        adapter.updateItem("b")
        adapter.updateItem("missing")
        waitUntil { adapter.currentList == listOf("a", "b", "c") }

        recycler.layoutItems()
        assertEquals(listOf("a", "b", "c"), recycler.texts())
    }

    @Test
    fun `a click without listeners is ignored`() {
        val adapter = DefaultClickAdapter()
        val recycler = recycler(adapter)
        adapter.updateList(listOf("a"))
        recycler.layoutItems()

        assertTrue(recycler.getChildAt(0).performClick())
        recycler.clearAdapters()
        assertNull(recycler.adapter)
    }

    @Test
    fun `the multi adapter creates a holder for each view type`() {
        val adapter = MultiAdapter()
        val recycler = recycler(adapter)
        val clicks = mutableListOf<Int>()
        adapter.setOnItemClickListener { index, _ -> clicks += index }

        adapter.updateList(listOf("a", "b"))
        recycler.layoutItems()

        assertEquals(listOf("0:a", "1:b"), recycler.texts())
        recycler.getChildAt(0).performClick()
        assertEquals(listOf("a"), adapter.clicked)
        assertEquals(listOf(0), clicks)
    }

    @Test
    fun `the diff callbacks compare the items`() {
        val default = EmaBaseRecyclerAdapter.getDefaultDiffCallback<String>()
        assertTrue(default.areItemsTheSame("a", "a"))
        assertFalse(default.areItemsTheSame("a", "b"))
        assertTrue(default.areContentsTheSame("a", "a"))
        assertFalse(default.areContentsTheSame("a", "b"))

        val always = EmaBaseRecyclerAdapter.getAlwaysUpdateCallback<String>()
        assertFalse(always.areItemsTheSame("a", "a"))
        assertFalse(always.areContentsTheSame("a", "a"))
    }

    private fun RecyclerView.offsets(decoration: RecyclerView.ItemDecoration) = (0 until childCount).map {
        Rect().also { rect -> decoration.getItemOffsets(rect, getChildAt(it), this, RecyclerView.State()) }
    }

    @Test
    fun `the vertical decoration separates the items`() {
        val adapter = TextAdapter()
        val recycler = recycler(adapter)
        adapter.updateList(listOf("a", "b", "c"))
        recycler.layoutItems()

        assertEquals(listOf(10, 10, 0), recycler.offsets(EmaVerticalSpaceDecoration(10)).map { it.bottom })
        assertEquals(
            listOf(10, 10, 10),
            recycler.offsets(EmaVerticalSpaceDecoration(10, addSpaceBelowLastItem = true)).map { it.bottom }
        )
    }

    @Test
    fun `the grid decoration spaces the columns`() {
        val adapter = TextAdapter()
        val recycler = recycler(adapter, GridLayoutManager(activity, 2))
        adapter.updateList(listOf("a", "b", "c", "d"))
        recycler.layoutItems()

        val withEdge = recycler.offsets(EmaGridSpacingItemDecoration(2, 10, includeEdge = true))
        assertEquals(Rect(10, 10, 5, 10), withEdge[0])
        assertEquals(Rect(5, 0, 10, 10), withEdge[3])

        val withoutEdge = recycler.offsets(EmaGridSpacingItemDecoration(2, 10, includeEdge = false))
        assertEquals(Rect(0, 0, 5, 0), withoutEdge[0])
        assertEquals(Rect(5, 10, 0, 0), withoutEdge[3])
    }

    @Test
    fun `the swipe callback draws its background and content and notifies the swipe`() {
        val adapter = TextAdapter()
        val recycler = recycler(adapter)
        adapter.updateList(listOf("a", "b"))
        recycler.layoutItems()
        val holder = recycler.findViewHolderForAdapterPosition(1)!!
        val canvas = Canvas(Bitmap.createBitmap(1000, 2000, Bitmap.Config.ARGB_8888))
        val icon = GradientDrawable().apply { setSize(20, 20) }
        val swipes = mutableListOf<Pair<Int, Int>>()
        val background = ColorDrawable(Color.RED)
        val iconCallback = EmaSwipeToDeleteCallback(
            background = background,
            type = EmaSwipeToDeleteCallback.Type.Icon(icon),
            paddingLeft = 2
        ) { direction, position -> swipes += direction to position }

        assertTrue(iconCallback.onMove(recycler, holder, holder))
        iconCallback.onChildDraw(canvas, recycler, holder, 50f, 0f, ItemTouchHelper.ACTION_STATE_SWIPE, true)
        assertTrue(iconCallback.isSwiping)
        assertEquals(20, icon.bounds.width())
        assertEquals(holder.itemView.left + 2, background.bounds.left)

        iconCallback.onChildDraw(canvas, recycler, holder, -50f, 0f, ItemTouchHelper.ACTION_STATE_SWIPE, true)
        assertEquals(20, icon.bounds.width())
        assertEquals(holder.itemView.right, background.bounds.right)

        iconCallback.onChildDraw(canvas, recycler, holder, 0f, 0f, ItemTouchHelper.ACTION_STATE_SWIPE, false)
        assertFalse(iconCallback.isSwiping)

        iconCallback.onSwiped(holder, ItemTouchHelper.LEFT)
        assertEquals(listOf(ItemTouchHelper.LEFT to 1), swipes)

        val textCallback = EmaSwipeToDeleteCallback(
            type = EmaSwipeToDeleteCallback.Type.Text(activity, "Delete", Color.WHITE, font = Typeface.DEFAULT)
        ) { _, _ -> }
        textCallback.onChildDraw(canvas, recycler, holder, 50f, 0f, ItemTouchHelper.ACTION_STATE_SWIPE, true)
        textCallback.onChildDraw(canvas, recycler, holder, -50f, 0f, ItemTouchHelper.ACTION_STATE_SWIPE, true)
        assertTrue(textCallback.isSwiping)

        val plainCallback = EmaSwipeToDeleteCallback { _, _ -> }
        plainCallback.onChildDraw(canvas, recycler, holder, 50f, 0f, ItemTouchHelper.ACTION_STATE_SWIPE, true)
        assertTrue(plainCallback.isSwiping)
    }
}
