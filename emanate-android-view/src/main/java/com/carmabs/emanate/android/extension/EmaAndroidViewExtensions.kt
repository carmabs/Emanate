@file:Suppress("DEPRECATION")

package com.carmabs.emanate.android.extension

import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.carmabs.emanate.android.base.EmaCoreFragment
import com.carmabs.emanate.android.ui.EmaAndroidView
import com.carmabs.emanate.android.viewmodel.EmaAndroidViewModel
import com.carmabs.emanate.core.savestate.EmaStateStore
import com.carmabs.emanate.core.state.EmaEvent
import com.carmabs.emanate.core.state.EmaState
import com.carmabs.emanate.core.viewmodel.EmaViewModelContract

/**
 * Created by Carlos Mateo Benito on 13/02/2021.
 *
 * <p>
 * Copyright (c) 2021 by Carmabs. All rights reserved.
 * </p>
 *
 * @author <a href=“mailto:apps.carmabs@gmail.com”>Carlos Mateo Benito</a>
 */
@Suppress("ClassName")
internal fun <S : EmaState, VM : EmaViewModelContract<S, E>, E : EmaEvent> EmaAndroidView<S, VM, E>.generateViewModel(
    vm: (store: EmaStateStore) -> VM
): EmaAndroidViewModel<S, E> {
    val fragmentScope =
        (this as? EmaCoreFragment<*, *, *>)?.fragmentViewModelScope ?: false

    val newVm = if (fragmentScope) {
        val fragment = this as Fragment
        initializeViewModel(fragment, vm)
    } else {
        val activity: FragmentActivity = when (this) {
            is Fragment -> requireActivity()
            is FragmentActivity -> this
            is View -> context as FragmentActivity
            else -> throw IllegalAccessException("The view must be contained inside a FragmentActivity lifecycle")
        }
        initializeViewModel(activity, vm)
    }

    return newVm
}
