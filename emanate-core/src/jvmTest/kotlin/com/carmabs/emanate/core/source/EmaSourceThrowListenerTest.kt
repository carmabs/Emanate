package com.carmabs.emanate.core.source

import com.carmabs.emanate.core.action.EmaAction
import com.carmabs.emanate.core.configurationOn
import com.carmabs.emanate.core.error.EmaError
import com.carmabs.emanate.core.error.EmaErrorPolicy
import com.carmabs.emanate.core.model.EmaConfiguration
import com.carmabs.emanate.core.sideeffect.EmaSideEffectListener
import com.carmabs.emanate.core.state.EmaEvent
import com.carmabs.emanate.core.state.EmaState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

private data object ThrowListenerState : EmaState

private data object FailingAction : EmaAction

/**
 * runTest rethrows the exceptions that no coroutine catches when it finishes, which only can be checked in the JVM
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EmaSourceThrowListenerTest {

    @Test
    fun `the listener receives the error of a side effect before it is thrown again with Throw`() {
        val errors = mutableListOf<EmaError>()
        val listener = object : EmaSideEffectListener {
            override fun onError(error: EmaError, duration: Duration) {
                errors += error
            }
        }

        assertFailsWith<IllegalStateException> {
            runTest {
                val source = EmaSource<ThrowListenerState, FailingAction, EmaEvent.EMPTY>(
                    ThrowListenerState,
                    configurationOn(
                        UnconfinedTestDispatcher(testScheduler),
                        EmaConfiguration(
                            errorPolicy = EmaErrorPolicy.Throw,
                            sideEffectListener = listener,
                            logger = null
                        )
                    )
                ) {
                    sideEffects {
                        add<FailingAction> { error("broken effect") }
                    }
                }
                source.dispatch(FailingAction)
                runCurrent()
            }
        }

        assertEquals("broken effect", errors.single().exception.message)
        assertEquals("FailingAction", errors.single().reflection.name)
    }
}
