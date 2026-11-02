package com.carmabs.emanate.core.source

import com.carmabs.emanate.core.action.EmaAction
import com.carmabs.emanate.core.configurationOn
import com.carmabs.emanate.core.logging.EmaLogger
import com.carmabs.emanate.core.source.middleware.loggerMiddleware
import com.carmabs.emanate.core.state.EmaEvent
import com.carmabs.emanate.core.state.EmaState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

private data class LoggedState(val count: Int = 0) : EmaState

private sealed interface LoggedAction : EmaAction {
    data object Increment : LoggedAction
}

private sealed interface LoggedEvent : EmaEvent

@OptIn(ExperimentalCoroutinesApi::class)
class EmaLoggerTest {

    @Test
    fun `the logger middleware logs the action with the state before and after it`() = runTest {
        val logs = mutableListOf<String>()
        val source = EmaSource<LoggedState, LoggedAction, LoggedEvent>(
            LoggedState(),
            configurationOn(UnconfinedTestDispatcher(testScheduler))
        ) {
            middlewares {
                add(loggerMiddleware("Counter", EmaLogger { tag, message -> logs += "$tag: $message" }))
            }
            reducers {
                add<LoggedAction.Increment> { state.copy(count = state.count + 1) }
            }
        }

        source.dispatch(LoggedAction.Increment)
        runCurrent()

        assertTrue(logs.isNotEmpty())
        assertTrue(logs.all { it.startsWith("Counter: ") })
        assertTrue(logs.any { "Increment" in it })
        assertTrue(logs.any { "count=0" in it })
        assertTrue(logs.any { "count=1" in it })
        source.close()
    }

    @Test
    fun `the prefix of the logger is added to the tag`() {
        val tags = mutableListOf<String>()
        val logger = EmaLogger(prefix = "PREFIX_") { tag, _ -> tags += tag }

        logger.log("Tag", "message")

        assertEquals(listOf("PREFIX_Tag"), tags)
    }

    @Test
    fun `a logger without prefix keeps the tag`() {
        val tags = mutableListOf<String>()
        val logger = EmaLogger(prefix = null) { tag, _ -> tags += tag }

        logger.log("Tag", "message")

        assertEquals(listOf("Tag"), tags)
    }
}
