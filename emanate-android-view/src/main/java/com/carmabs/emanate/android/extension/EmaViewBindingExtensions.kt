@file:Suppress("DEPRECATION")

package com.carmabs.emanate.android.extension

import com.carmabs.emanate.core.state.EmaState
import com.carmabs.emanate.core.view.EmaView
import kotlin.jvm.internal.PropertyReference0
import kotlin.reflect.KProperty

/*
 * Extensions to render only the fields of the state that have changed. They read the previous value of the
 * field with Java reflection, so they are available only on Android/JVM.
 *
 * @author <a href=“mailto:apps.carmabs@gmail.com”>Carlos Mateo Benito</a>
 */

/**
 * Check EMA state selected property to execute action with new value if it has changed
 * @param action Action to execute. Current value passed in lambda.
 * @param field Ema State field to check if it has been changed.
 * @param areEqualComparator Comparator to determine if both objects are equals. Useful for complex objects
 * @return true if it has been updated, false otherwise
 */
@Suppress("UNCHECKED_CAST")
fun <S : EmaState, T> EmaView<S, *, *>.bindForUpdate(
    field: KProperty<T>,
    areEqualComparator: ((old: T, new: T) -> Boolean)? = null,
    action: (new: T) -> Unit
): Boolean {
    var updated = false
    val currentClass = (field as PropertyReference0).boundReceiver as? S
    currentClass?.also { _ ->
        val currentValue = (field.get() as T)
        previousState?.also {
            try {
                val previousField = it.javaClass.getDeclaredField(field.name)
                previousField.isAccessible = true
                val previousValue = previousField.get(previousState) as T
                if (areEqualComparator?.invoke(previousValue, currentValue)?.not()
                    ?: (previousValue != currentValue)
                ) {
                    updated = true
                    action.invoke(currentValue)
                }
            } catch (e: Exception) {
                println("EMA : Field not found")
            }
        } ?: action.invoke(currentValue)
    } ?: println("EMA : Bounding class must be the state of the view")
    return updated
}

/**
 * Check EMA state selected property to execute action with new value if it has changed
 * @param action Action to execute. Current and previous value passed in lambda
 * @param field Ema State field to check if it has been changed
 * @param areEqualComparator Comparator to determine if both objects are equals. Useful for complex objects
 * @return true if it has been updated, false otherwise
 */
@Suppress("UNCHECKED_CAST")
fun <S : EmaState, T> EmaView<S, *, *>.bindForUpdateWithPrevious(
    field: KProperty<T>,
    areEqualComparator: ((old: T, new: T) -> Boolean)? = null,
    action: (old: T?, new: T) -> Unit
): Boolean {
    var updated = false
    val currentClass = (field as PropertyReference0).boundReceiver as? S
    currentClass?.also { _ ->
        val currentValue = (field.get() as T)
        previousState?.also {
            try {
                val previousField = it.javaClass.getDeclaredField(field.name)
                previousField.isAccessible = true
                val previousValue = previousField.get(previousState) as T
                if (areEqualComparator?.invoke(previousValue, currentValue)?.not()
                    ?: (previousValue != currentValue)
                ) {
                    updated = true
                    action.invoke(previousValue, currentValue)
                }
            } catch (e: Exception) {
                println("EMA : Field not found")
            }
        } ?: action.invoke(null, currentValue)
    } ?: println("EMA : Bounding class must be the state of the view")
    return updated
}
