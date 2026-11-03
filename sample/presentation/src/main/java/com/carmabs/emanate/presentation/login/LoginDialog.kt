package com.carmabs.emanate.presentation.login

/**
 * Created by Carlos Mateo Benito on 22/3/24.
 *
 * <p>
 * Copyright (c) 2024 by Carmabs. All rights reserved.
 * </p>
 *
 * @author <a href=“mailto:apps.carmabs@gmail.com”>Carlos Mateo Benito</a>
 */
sealed interface LoginDialog {
    data object ErrorBadCredentials : LoginDialog
}
