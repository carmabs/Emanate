package com.carmabs.emanate.presentation.profile.creation

/**
 * Created by Carlos Mateo Benito on 23/3/24.
 *
 * <p>
 * Copyright (c) 2024 by Carmabs. All rights reserved.
 * </p>
 *
 * @author <a href=“mailto:apps.carmabs@gmail.com”>Carlos Mateo Benito</a>
 */
sealed interface ProfileCreationDialog {

    data object BackConfirmation : ProfileCreationDialog

    data object CreateConfirmation : ProfileCreationDialog

    data object CreateError : ProfileCreationDialog
}
