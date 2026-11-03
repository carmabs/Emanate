package com.carmabs.emanate.core.source

import com.carmabs.emanate.core.action.EmaAction
import com.carmabs.emanate.core.configurationOn
import com.carmabs.emanate.core.state.EmaEvent
import com.carmabs.emanate.core.state.EmaState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

private data class TextState(val text: String = "") : EmaState

private sealed interface TextAction : EmaAction {
    data object Write : TextAction
}

/**
 * Actions of [StampPlugin]
 */
private sealed interface StampAction : EmaAction {
    data object Stamped : StampAction
}

/**
 * Plugin that appends [suffix] to the text with every action
 */
private class SuffixPlugin(private val suffix: String) : EmaSourcePlugin<TextState, Nothing> {
    override fun install(setup: SourceSetupScope<TextState, Nothing, Nothing>) {
        setup.reducers {
            add<TextAction.Write> { state.copy(text = state.text + suffix) }
        }
    }
}

/**
 * Plugin that dispatches its own action after every Write
 */
private class StampPlugin : EmaSourcePlugin<TextState, StampAction> {
    override fun install(setup: SourceSetupScope<TextState, StampAction, Nothing>) {
        setup.sideEffects {
            add<TextAction.Write> { dispatch(StampAction.Stamped) }
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class EmaSourcePluginTest {

    private fun TestScope.textSource(setup: SourceSetupScope<TextState, TextAction, EmaEvent.EMPTY>.() -> Unit) =
        EmaSource(TextState(), configurationOn(UnconfinedTestDispatcher(testScheduler)), setup)

    @Test
    fun `the plugins add their pieces in the order they are added`() = runTest {
        val source = textSource {
            plugins {
                add(SuffixPlugin("a"))
                add(SuffixPlugin("b"))
                add(SuffixPlugin("c"))
            }
        }

        source.dispatch(TextAction.Write)
        runCurrent()

        assertEquals("abc", source.state.text)
        source.close()
    }

    @Test
    fun `several plugins are added at once in the order they are given`() = runTest {
        val source = textSource {
            plugins {
                add(SuffixPlugin("a"), SuffixPlugin("b"))
                add(StampPlugin(), StampPlugin())
                add(SuffixPlugin("c"))
            }
        }

        source.dispatch(TextAction.Write)
        runCurrent()

        assertEquals("abc", source.state.text)
        source.close()
    }

    @Test
    fun `the pieces of a plugin keep their place with the ones of the setup`() = runTest {
        val source = textSource {
            reducers {
                add<TextAction.Write> { state.copy(text = state.text + "1") }
            }
            plugins {
                add(SuffixPlugin("a"))
            }
            reducers {
                add<TextAction.Write> { state.copy(text = state.text + "2") }
            }
        }

        source.dispatch(TextAction.Write)
        runCurrent()

        assertEquals("1a2", source.state.text)
        source.close()
    }

    @Test
    fun `the source receives the actions dispatched by a plugin`() = runTest {
        val source = textSource {
            plugins {
                add(StampPlugin())
            }
            reducers {
                add<TextAction.Write> { state.copy(text = state.text + "a") }
                add<StampAction.Stamped> { state.copy(text = state.text + "!") }
            }
        }

        source.dispatch(TextAction.Write)
        runCurrent()

        assertEquals("a!", source.state.text)
        source.close()
    }

    @Test
    fun `the setup knows the type of the actions of each plugin`() {
        val setup = buildSourceSetup<TextState, TextAction, EmaEvent.EMPTY> {
            plugins {
                add(SuffixPlugin("a"))
                add(StampPlugin())
            }
        }

        assertEquals(listOf(StampAction::class), setup.pluginActionTypes)
    }

    @Test
    fun `a plugin can't be added after setup`() = runTest {
        lateinit var scope: PluginSetupScope<TextState>
        val source = textSource {
            plugins { scope = this }
        }

        assertFailsWith<IllegalStateException> { with(scope) { add(SuffixPlugin("a")) } }
        source.close()
    }
}
