package com.carmabs.emanate.android.configuration

import com.carmabs.emanate.android.logging.EmaAndroidDataClassPrinter
import com.carmabs.emanate.android.logging.Logcat
import com.carmabs.emanate.core.concurrency.EmaDispatchers
import com.carmabs.emanate.core.error.EmaErrorPolicy
import com.carmabs.emanate.core.logging.EmaLogger
import com.carmabs.emanate.core.model.EmaConfiguration
import kotlinx.coroutines.Dispatchers

/**
 * Emanate configuration for Android:
 * - ViewModels run on [Dispatchers.Main.immediate], so the work started from the main thread runs at once, and use
 *   cases on `Dispatchers.IO`.
 * - Objects are printed with their fields.
 * - In debug builds the logs go to logcat and the exceptions are thrown, so the app fails at once. In release builds
 *   there are no logs, so the state doesn't reach logcat, and the exceptions are caught.
 *
 * Customize it with `copy`, for example `EmaConfiguration.Android(BuildConfig.DEBUG).copy(errorPolicy = ...)`.
 *
 * @param debug whether the app is a debug build, usually BuildConfig.DEBUG
 */
@Suppress("FunctionName") // Named like a predefined configuration
fun EmaConfiguration.Companion.Android(debug: Boolean): EmaConfiguration = EmaConfiguration(
    dispatchers = EmaDispatchers(main = Dispatchers.Main.immediate),
    errorPolicy = if (debug) EmaErrorPolicy.Throw else EmaErrorPolicy.Catch(),
    logger = if (debug) EmaLogger.Logcat else null,
    printer = EmaAndroidDataClassPrinter()
)
