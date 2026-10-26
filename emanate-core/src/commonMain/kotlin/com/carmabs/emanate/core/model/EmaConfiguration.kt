package com.carmabs.emanate.core.model

import com.carmabs.emanate.core.broadcast.EmaActionBroadcaster
import com.carmabs.emanate.core.concurrency.EmaDispatchers
import com.carmabs.emanate.core.error.EmaErrorPolicy
import com.carmabs.emanate.core.logging.EmaDataClassPrinter
import com.carmabs.emanate.core.logging.EmaLogger
import com.carmabs.emanate.core.sideeffect.EmaSideEffectListener
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Configuration of Emanate, set for the whole application with [com.carmabs.emanate.core.Emanate.init] or passed to
 * a ViewModel or a source. Each property configures one thing, so customize the one you need with copy.
 *
 * @param dispatchers where ViewModels ([EmaDispatchers.main]), use cases ([EmaDispatchers.io]) and CPU-intensive work
 * ([EmaDispatchers.default]) run by default
 * @param errorPolicy what happens when a middleware, reducer or side effect throws an exception
 * @param logger writes the logs of Emanate, like the actions of every EmaViewModel with the state before and after
 * them, and the errors caught by [errorPolicy]. Null to disable the logs, for example in release builds.
 * @param printer how objects are printed in the logs and by [com.carmabs.emanate.core.logging.toStringPretty]
 * @param closeTimeout time the onClose blocks of a source and its side effects that survive close can run after it is
 * closed. Then they are cancelled, so they don't keep the source alive.
 * @param sideEffectListener receives the result of every side effect, to track or measure them
 * @param broadcaster sends the actions broadcast by the view models and sources to the ones listening to their type,
 * if they use the same broadcaster. By default [EmaActionBroadcaster.Shared], used by the whole process; create a new
 * one to isolate the view models and sources of this configuration, for example in a library.
 *
 * @author <a href=“mailto:apps.carmabs@gmail.com”>Carlos Mateo Benito</a>
 */
data class EmaConfiguration(
    val dispatchers: EmaDispatchers = EmaDispatchers(),
    val errorPolicy: EmaErrorPolicy = EmaErrorPolicy.Catch(),
    val logger: EmaLogger? = EmaLogger.Println,
    val printer: EmaDataClassPrinter = EmaDataClassPrinter.ToString,
    val closeTimeout: Duration = 5.seconds,
    val sideEffectListener: EmaSideEffectListener? = null,
    val broadcaster: EmaActionBroadcaster = EmaActionBroadcaster.Shared
) {
    // Empty, so other modules can add predefined configurations as extensions, like EmaConfiguration.Android
    companion object
}
