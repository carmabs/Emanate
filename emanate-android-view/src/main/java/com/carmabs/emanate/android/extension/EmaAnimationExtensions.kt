@file:Suppress("DEPRECATION")

package com.carmabs.emanate.android.extension

import android.animation.Animator
import android.animation.AnimatorSet
import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.TransitionDrawable
import android.os.Build
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.OvershootInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.annotation.ColorInt
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import com.carmabs.emanate.android.ANIMATION_DURATION
import com.carmabs.emanate.android.ANIMATION_OVERSHOOT
import com.carmabs.emanate.core.constants.FLOAT_ONE
import com.carmabs.emanate.core.constants.FLOAT_ZERO

/*
 * Extensions for animation
 *
 * @author <a href=“mailto:apps.carmabs@gmail.com”>Carlos Mateo</a>
 */

/**
 * Pause the animation and its children
 */
fun Animator.pauseAll() {
    (this as? AnimatorSet)?.apply {
        childAnimations.forEach { animatorChild ->
            animatorChild.pauseAll()
        }
    }
    pause()
}

/**
 * Resume the animation and its children
 */
fun Animator.resumeAll() {
    (this as? AnimatorSet)?.apply {
        childAnimations.forEach { animatorChild ->
            animatorChild.resumeAll()
        }
    }
    resume()
}

/**
 * End the animation and its children
 */
fun Animator.endAll() {
    (this as? AnimatorSet)?.apply {
        childAnimations.forEach { animatorChild ->
            animatorChild.endAll()
        }
    }
    end()
}

/**
 * Cancel the animation and its children
 */
fun Animator.cancelAll() {
    (this as? AnimatorSet)?.apply {
        childAnimations.forEach { animatorChild ->
            animatorChild.cancelAll()
        }
    }
    cancel()
}

/**
 * Check if the animation or some of its children is running
 */
fun Animator.isSomeRunning(): Boolean {
    var isChildRunning = false
    (this as? AnimatorSet)?.apply {
        childAnimations.forEach { animatorChild ->
            isChildRunning = animatorChild.isSomeRunning()
            if (isChildRunning) {
                return@apply
            }
        }
    }
    return isChildRunning || isRunning
}
