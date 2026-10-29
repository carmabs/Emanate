package com.carmabs.emanate.test

import com.carmabs.emanate.core.Emanate
import com.carmabs.emanate.core.broadcast.EmaActionBroadcaster
import com.carmabs.emanate.core.concurrency.EmaDispatchers
import com.carmabs.emanate.core.model.EmaConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

/**
 * Returns [configuration] running on [dispatcher], to pass it to the view models and sources of a test instead of
 * calling [useTestDispatchers]. It doesn't change the configuration of Emanate or `Dispatchers.Main`, so the tests that
 * use it can run in parallel and don't need to reset anything.
 *
 * If [configuration] uses the shared broadcaster, it gets a new one, so the actions broadcast in the test don't reach
 * the other tests. The view models and sources of a test that broadcast actions to each other have to receive the same
 * configuration.
 *
 * Create [dispatcher] with the scheduler of runTest, so advanceUntilIdle runs the work of the view models, delays
 * included: `testConfiguration(UnconfinedTestDispatcher(testScheduler))`.
 *
 * @param dispatcher where the view models, sources and use cases run. With an UnconfinedTestDispatcher the actions are
 * processed as soon as they are dispatched; with a StandardTestDispatcher, when the test advances.
 * @param configuration the rest of the configuration. By default the default one, not the one of Emanate, so the test
 * doesn't depend on [Emanate.init].
 *
 * @author <a href=“mailto:apps.carmabs@gmail.com”>Carlos Mateo Benito</a>
 */
fun testConfiguration(
    dispatcher: TestDispatcher,
    configuration: EmaConfiguration = EmaConfiguration()
): EmaConfiguration = configuration.copy(
    dispatchers = EmaDispatchers(main = dispatcher, io = dispatcher, default = dispatcher),
    broadcaster = configuration.broadcaster.takeUnless { it === EmaActionBroadcaster.Shared } ?: EmaActionBroadcaster()
)

/**
 * Configuration of Emanate before [useTestDispatchers], restored by [resetTestDispatchers]
 */
private var configurationBeforeTest: EmaConfiguration? = null

/**
 * Runs the view models, sources and use cases created after it on [dispatcher]. Call it before each test, for example
 * in a function annotated with BeforeTest, and [resetTestDispatchers] after it. It changes the configuration of
 * Emanate, shared by the whole process, so the tests that use it can't run in parallel; [testConfiguration] doesn't.
 *
 * It also sets [dispatcher] as `Dispatchers.Main`, so runTest uses its scheduler: advanceUntilIdle runs the work of the
 * view models, delays included.
 *
 * @param dispatcher by default the actions are processed as soon as they are dispatched. With a
 * StandardTestDispatcher they are processed when the test advances.
 * @return [dispatcher]
 *
 * @author <a href=“mailto:apps.carmabs@gmail.com”>Carlos Mateo Benito</a>
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun useTestDispatchers(dispatcher: TestDispatcher = UnconfinedTestDispatcher()): TestDispatcher {
    if (configurationBeforeTest == null) configurationBeforeTest = Emanate.configuration
    Dispatchers.setMain(dispatcher)
    Emanate.initForTest(
        Emanate.configuration.copy(
            dispatchers = EmaDispatchers(main = dispatcher, io = dispatcher, default = dispatcher)
        )
    )
    return dispatcher
}

/**
 * Undoes [useTestDispatchers]: restores `Dispatchers.Main` and the configuration of Emanate, and removes the actions
 * broadcast with keepUntilReceived through the shared broadcaster that nobody received, so they don't reach the next
 * test. Call it after each test, for example in a function annotated with AfterTest.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun resetTestDispatchers() {
    Dispatchers.resetMain()
    configurationBeforeTest?.let { Emanate.initForTest(it) }
    configurationBeforeTest = null
    EmaActionBroadcaster.Shared.clearPendingActions()
}
