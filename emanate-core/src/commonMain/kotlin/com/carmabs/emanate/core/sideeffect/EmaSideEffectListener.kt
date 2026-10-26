package com.carmabs.emanate.core.sideeffect

import com.carmabs.emanate.core.model.reflection.Reflection
import com.carmabs.emanate.core.error.EmaError
import com.carmabs.emanate.core.error.EmaErrorPolicy
import kotlin.time.Duration

/**
 * Created by Carlos Mateo Benito on 12/10/2026.
 *
 * Receives the result of every side effect, set in
 * [com.carmabs.emanate.core.model.EmaConfiguration.sideEffectListener]. Useful to track or measure them. It is
 * called for the side effects of the view models and the sources, and each function receives the time that the side
 * effect took. The errors are also handled by the error policy of the configuration.
 *
 * <p>
 * Copyright (c) 2026 by Carmabs. All rights reserved.
 * </p>
 *
 * @author <a href=“mailto:apps.carmabs@gmail.com”>Carlos Mateo Benito</a>
 */
interface EmaSideEffectListener {

    /**
     * A side effect has finished successfully
     *
     * @param output what it has launched while it ran: the actions, broadcasts and events. It is empty for the work
     * that doesn't launch any
     * @param duration time that the side effect took
     */
    fun onSuccess(reflection: Reflection, output: SideEffectOutput, duration: Duration) = Unit

    /**
     * A side effect has thrown [error]. It is called with every error policy: with [EmaErrorPolicy.Throw] right before
     * the exception is thrown again. A cancellation is not an error.
     *
     * @param duration time that the side effect took until it failed
     */
    fun onError(error: EmaError, duration: Duration) = Unit

    /**
     * A side effect has finished, with or without error, or has been cancelled
     *
     * @param duration time that the side effect took
     */
    fun onFinish(reflection: Reflection, duration: Duration) = Unit
}
