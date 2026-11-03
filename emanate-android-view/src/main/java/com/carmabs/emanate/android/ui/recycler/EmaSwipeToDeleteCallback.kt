@file:Suppress("DEPRECATION")

package com.carmabs.emanate.android.ui.recycler

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.view.View
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.carmabs.emanate.android.extension.dp
import com.carmabs.emanate.core.constants.FLOAT_ZERO
import com.carmabs.emanate.core.constants.INT_ZERO

/**
 * Created by Carlos Mateo Benito on 10/11/21.
 *
 * <p>
 * Copyright (c) 2021 by Carmabs. All rights reserved.
 * </p>
 *
 * @author <a href=“mailto:apps.carmabs@gmail.com”>Carlos Mateo Benito</a>
 */
@Deprecated(
    "Emanate will work only with Compose and EmaViewModel. Build the screens with emanate-compose-android and EmaViewModel instead of Views."
)
class EmaSwipeToDeleteCallback(
    swipeDirs: Int = ItemTouchHelper.LEFT,
    private val background: Drawable? = null,
    private val backgroundCornerOffset: Int = INT_ZERO,
    private val type: Type? = null,
    private val paddingTop: Int = INT_ZERO,
    private val paddingLeft: Int = INT_ZERO,
    private val paddingBottom: Int = INT_ZERO,
    private val paddingRight: Int = INT_ZERO,
    private val onSwipeAction: (direction: Int, itemPosition: Int) -> Unit
) : ItemTouchHelper.SimpleCallback(INT_ZERO, swipeDirs) {

    private var dx = FLOAT_ZERO

    val isSwiping: Boolean
        get() = dx != FLOAT_ZERO

    private val paintText by lazy {
        Paint().apply {
            isAntiAlias = true
        }
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean = true

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        dx = FLOAT_ZERO
        onSwipeAction.invoke(direction, viewHolder.adapterPosition)
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        val itemView = viewHolder.itemView
        drawBackground(itemView, dX, c)
        when (type) {
            is Type.Icon -> drawIcon(type, itemView, dX, c)

            is Type.Text -> drawText(type, itemView, dX, c)

            null -> {
                // DO NOTHING
            }
        }
        this.dx = dX
    }

    private fun drawBackground(itemView: View, dX: Float, canvas: Canvas) {
        background?.also {
            when {
                dX > INT_ZERO -> { // Swiping to the right
                    it.setBounds(
                        itemView.left + paddingLeft,
                        itemView.top + paddingTop,
                        // itemView.left + dX.toInt() + backgroundCornerOffset, Dynamic
                        itemView.right + backgroundCornerOffset - paddingRight, // Static
                        itemView.bottom - paddingBottom
                    )
                }

                dX < INT_ZERO -> { // Swiping to the left
                    background.setBounds(
                        // itemView.right + dX.toInt() - backgroundCornerOffset, Dynamic
                        itemView.left - backgroundCornerOffset + paddingLeft,
                        itemView.top + paddingTop,
                        itemView.right - paddingRight,
                        itemView.bottom - paddingBottom
                    )
                }

                else -> { // view is unSwiped
                    background.setBounds(
                        INT_ZERO + paddingLeft,
                        INT_ZERO + paddingTop,
                        INT_ZERO - paddingRight,
                        INT_ZERO - paddingBottom
                    )
                }
            }
            it.draw(canvas)
        }
    }

    private fun drawIcon(typeIcon: Type.Icon, itemView: View, dX: Float, canvas: Canvas) {
        val icon = typeIcon.drawable
        val iconMargin: Int = (itemView.height - icon.intrinsicHeight) / 4
        val iconTop: Int = itemView.top + (itemView.height - icon.intrinsicHeight) / 2
        val iconBottom = iconTop + icon.intrinsicHeight

        when {
            dX > INT_ZERO -> { // Swiping to the right
                val iconLeft: Int = itemView.left + iconMargin
                val iconRight: Int = itemView.left + iconMargin + icon.intrinsicWidth
                icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
            }

            dX < INT_ZERO -> { // Swiping to the left
                val iconLeft: Int = itemView.right - iconMargin - icon.intrinsicWidth
                val iconRight: Int = itemView.right - iconMargin
                icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
            }
        }
        icon.draw(canvas)
    }

    private fun drawText(typeText: Type.Text, itemView: View, dX: Float, canvas: Canvas) {
        val textMargin: Int = (itemView.height) / 4
        val textCenter: Int = itemView.top + (itemView.height) / 2
        paintText.color = typeText.color
        paintText.textSize = typeText.textSize
        paintText.typeface = typeText.font

        val textX = when {
            dX > INT_ZERO -> { // Swiping to the right
                paintText.textAlign = Paint.Align.LEFT
                itemView.left + textMargin
            }

            else -> { // Swiping to the left
                paintText.textAlign = Paint.Align.RIGHT
                itemView.right - textMargin
            }
        }
        canvas.drawText(
            typeText.text,
            textX.toFloat(),
            textCenter + (paintText.ascent() + paintText.descent()) / 2,
            paintText
        )
    }

    sealed interface Type {
        data class Icon(val drawable: Drawable) : Type
        data class Text(
            val context: Context,
            val text: String,
            @ColorInt val color: Int,
            val textSize: Float = 14.dp.toFloat(),
            val font: Typeface? = null
        ) : Type
    }
}
