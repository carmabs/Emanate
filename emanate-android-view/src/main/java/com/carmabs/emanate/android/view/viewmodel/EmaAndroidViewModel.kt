package com.carmabs.emanate.android.view.viewmodel

import androidx.annotation.CallSuper
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.carmabs.emanate.core.state.EmaEvent
import com.carmabs.emanate.core.state.EmaState
import com.carmabs.emanate.core.viewmodel.EmaViewModelContract

/**
 * Created by Carlos Mateo Benito on 13/02/2021.
 *
 * Android view model that keeps [emaViewModel] while its screen lives, configuration changes included. Its
 * factory gives [emaViewModel] a store over [savedStateHandle], so the backup of an EmaViewModel restores its values
 * after the system kills the process of the app.
 *
 * <p>
 * Copyright (c) 2021 by Carmabs. All rights reserved.
 * </p>
 *
 * @author <a href=“mailto:apps.carmabs@gmail.com”>Carlos Mateo Benito</a>
 */
open class EmaAndroidViewModel<S : EmaState, E : EmaEvent>(
    val emaViewModel: EmaViewModelContract<S, E>,
    val savedStateHandle: SavedStateHandle
) : ViewModel() {

    @CallSuper
    override fun onCleared() {
        emaViewModel.close()
        super.onCleared()
    }
}
