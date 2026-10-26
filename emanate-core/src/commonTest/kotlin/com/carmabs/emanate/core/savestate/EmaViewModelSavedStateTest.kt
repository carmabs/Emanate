package com.carmabs.emanate.core.savestate

import com.carmabs.emanate.core.Emanate
import com.carmabs.emanate.core.action.EmaAction
import com.carmabs.emanate.core.configurationOn
import com.carmabs.emanate.core.error.EmaError
import com.carmabs.emanate.core.error.EmaErrorPolicy
import com.carmabs.emanate.core.initializer.EmaInitializer
import com.carmabs.emanate.core.model.EmaConfiguration
import com.carmabs.emanate.core.source.EmaSource
import com.carmabs.emanate.core.state.EmaEvent
import com.carmabs.emanate.core.state.EmaState
import com.carmabs.emanate.core.viewmodel.EmaViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

private enum class Plan { FREE, PRO }

private data class FormState(
    val name: String = "",
    val age: Int = 0,
    val plan: Plan = Plan.FREE,
    val role: String = "",
    val greeting: String = ""
) : EmaState

private sealed interface FormAction : EmaAction {
    data class NameWritten(val name: String) : FormAction
    data class AgeWritten(val age: Int) : FormAction
    data class PlanChosen(val plan: Plan) : FormAction
}

private data class RoleInitializer(val role: String) : EmaInitializer

/**
 * Action after which a source on its own restores its values
 */
private data object Opened : EmaAction

/**
 * Store that keeps the values in memory and records every write
 */
private class MemoryStore(initialValues: Map<String, String> = emptyMap()) : EmaStateStore {

    val values = initialValues.toMutableMap()

    val writes = mutableListOf<Pair<String, String?>>()

    override fun read(key: String): String? = values[key]

    override fun write(key: String, value: String?) {
        writes += key to value
        if (value == null) values -= key else values[key] = value
    }
}

/**
 * Saves an enum by its name. A name that doesn't exist anymore isn't restored.
 */
private object PlanCodec : EmaBackupCodec<Plan> {
    override fun encode(value: Plan): String = value.name

    override fun decode(value: String): Plan? = Plan.entries.find { it.name == value }
}

private object IntCodec : EmaBackupCodec<Int> {
    override fun encode(value: Int): String = value.toString()

    override fun decode(value: String): Int? = value.toIntOrNull()
}

/**
 * Saved value implemented as a class, received in the constructor of the view model
 */
private class AgeSavedValue(private val failRestore: Boolean = false) : EmaBackupValue<FormState, Int> {

    override val key: String = "age"

    override val codec: EmaBackupCodec<Int> = IntCodec

    override fun save(state: FormState): Int = state.age

    override fun restore(state: FormState, value: Int): FormState {
        if (failRestore) error("broken restore")
        return state.copy(age = value)
    }
}

/**
 * Backup of the name, for the sources on their own
 */
private fun nameBackup(store: EmaStateStore) = backupOf<FormState>(store, restoreOn = Opened::class) {
    add("name", save = { name.takeIf { it.isNotEmpty() } }, restore = { copy(name = it) })
}

private class FormViewModel(
    dispatcher: CoroutineDispatcher,
    private val store: EmaStateStore,
    private val ageSavedValue: EmaBackupValue<FormState, Int> = AgeSavedValue(),
    configuration: EmaConfiguration = Emanate.configuration
) : EmaViewModel<FormState, FormAction, EmaEvent.EMPTY>(
    FormState(),
    configurationOn(dispatcher, configuration).copy(logger = null)
) {

    val received = mutableListOf<EmaAction>()

    override fun setup() = source {
        plugins {
            +backupOf(store) {
                add("name", save = { name.takeIf { it.isNotEmpty() } }, restore = { copy(name = it) })
                +ageSavedValue
                add("plan", PlanCodec, save = { plan }, restore = { copy(plan = it) })
            }
        }
        middlewares {
            add<EmaAction> { action ->
                received += action
                next()
            }
        }
        reducers {
            add<RoleInitializer> { initializer -> state.copy(role = initializer.role) }
            add<FormAction> { action ->
                when (action) {
                    is FormAction.NameWritten -> state.copy(name = action.name)
                    is FormAction.AgeWritten -> state.copy(age = action.age)
                    is FormAction.PlanChosen -> state.copy(plan = action.plan)
                }
            }
            // Receives the restored state
            add<EmaRestoreState> { state.copy(greeting = "Welcome back ${state.name}") }
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class EmaViewModelSavedStateTest {

    private val errors = mutableListOf<EmaError>()

    private fun TestScope.testDispatcher() = UnconfinedTestDispatcher(testScheduler)

    @Test
    fun `the saved values are written when they change`() = runTest {
        val store = MemoryStore()
        val viewModel = FormViewModel(testDispatcher(), store)

        viewModel.onCreated(null)
        viewModel.dispatch(FormAction.NameWritten("Ana"))
        viewModel.dispatch(FormAction.NameWritten("Ana"))
        viewModel.dispatch(FormAction.AgeWritten(30))
        viewModel.dispatch(FormAction.PlanChosen(Plan.PRO))
        runCurrent()

        assertEquals(
            listOf<Pair<String, String?>>(
                "age" to "0",
                "plan" to "FREE",
                "name" to "Ana",
                "age" to "30",
                "plan" to "PRO"
            ),
            store.writes
        )
        assertEquals(mapOf("name" to "Ana", "age" to "30", "plan" to "PRO"), store.values)
        viewModel.close()
    }

    @Test
    fun `a null value removes the saved one`() = runTest {
        val store = MemoryStore()
        val viewModel = FormViewModel(testDispatcher(), store)

        viewModel.onCreated(null)
        viewModel.dispatch(FormAction.NameWritten("Ana"))
        viewModel.dispatch(FormAction.NameWritten(""))
        runCurrent()

        assertEquals(mapOf("age" to "0", "plan" to "FREE"), store.values)
        assertEquals(
            listOf<Pair<String, String?>>("name" to "Ana", "name" to null),
            store.writes.filter { it.first == "name" }
        )
        viewModel.close()
    }

    @Test
    fun `the saved values are restored with an action after the initializer`() = runTest {
        val store = MemoryStore(mapOf("name" to "Ana", "age" to "30", "plan" to "PRO"))
        val viewModel = FormViewModel(testDispatcher(), store)

        viewModel.onCreated(RoleInitializer("admin"))
        runCurrent()

        assertEquals(
            listOf(
                RoleInitializer("admin"),
                EmaRestoreState(mapOf("name" to "Ana", "age" to "30", "plan" to "PRO"))
            ),
            viewModel.received
        )
        assertEquals(
            FormState(name = "Ana", age = 30, plan = Plan.PRO, role = "admin", greeting = "Welcome back Ana"),
            viewModel.stateFlow.value
        )
        viewModel.close()
    }

    @Test
    fun `the initializer doesn't overwrite the values before they are restored`() = runTest {
        val store = MemoryStore(mapOf("name" to "Ana", "age" to "30", "plan" to "PRO"))
        val viewModel = FormViewModel(testDispatcher(), store)

        viewModel.onCreated(RoleInitializer("admin"))
        runCurrent()

        assertTrue(store.writes.isEmpty())
        assertEquals(mapOf("name" to "Ana", "age" to "30", "plan" to "PRO"), store.values)
        viewModel.close()
    }

    @Test
    fun `a new view model with the same store restores the state of the previous one`() = runTest {
        val store = MemoryStore()
        val first = FormViewModel(testDispatcher(), store)
        first.onCreated(RoleInitializer("admin"))
        first.dispatch(FormAction.NameWritten("Ana"))
        first.dispatch(FormAction.AgeWritten(30))
        runCurrent()
        first.close()

        val second = FormViewModel(testDispatcher(), store)
        second.onCreated(RoleInitializer("admin"))
        runCurrent()

        assertEquals(
            FormState(name = "Ana", age = 30, role = "admin", greeting = "Welcome back Ana"),
            second.stateFlow.value
        )
        second.close()
    }

    @Test
    fun `a value that can't be decoded isn't restored and is saved again from the state`() = runTest {
        val store = MemoryStore(mapOf("plan" to "GOLD"))
        val viewModel = FormViewModel(testDispatcher(), store)

        viewModel.onCreated(null)
        runCurrent()

        assertEquals(FormState(greeting = "Welcome back "), viewModel.stateFlow.value)
        assertEquals(mapOf("age" to "0", "plan" to "FREE"), store.values)
        viewModel.close()
    }

    @Test
    fun `nothing is restored when the store is empty`() = runTest {
        val viewModel = FormViewModel(testDispatcher(), MemoryStore())

        viewModel.onCreated(null)
        runCurrent()

        assertEquals(listOf<EmaAction>(EmaInitializer.EMPTY), viewModel.received)
        viewModel.close()
    }

    @Test
    fun `with the store None nothing is saved nor restored`() = runTest {
        val viewModel = FormViewModel(testDispatcher(), EmaStateStore.None)

        viewModel.onCreated(null)
        viewModel.dispatch(FormAction.NameWritten("Ana"))
        runCurrent()

        assertEquals(FormState(name = "Ana"), viewModel.stateFlow.value)
        viewModel.close()
    }

    @Test
    fun `a restore that fails discards the restore like any reducer and is reported`() = runTest {
        val configuration = EmaConfiguration(errorPolicy = EmaErrorPolicy.Catch { errors += it })
        val store = MemoryStore(mapOf("name" to "Ana", "age" to "30"))
        val viewModel = FormViewModel(testDispatcher(), store, AgeSavedValue(failRestore = true), configuration)

        viewModel.onCreated(null)
        runCurrent()

        assertEquals(FormState(), viewModel.stateFlow.value)
        assertEquals("action EmaRestoreState", errors.single().reflection.name)
        viewModel.close()
    }

    @Test
    fun `a saved value can be replaced in the constructor`() = runTest {
        val fakeAge = savedValueOf<FormState, Int>("age", IntCodec, save = { 7 }, restore = { copy(age = 99) })
        val store = MemoryStore(mapOf("age" to "30"))
        val viewModel = FormViewModel(testDispatcher(), store, fakeAge)

        viewModel.onCreated(null)
        runCurrent()

        assertEquals(99, viewModel.stateFlow.value.age)
        assertEquals("7", store.values["age"])
        viewModel.close()
    }

    @Test
    fun `nothing is read nor written before the initializer`() = runTest {
        val store = object : EmaStateStore {
            val keys = mutableListOf<String>()

            override fun read(key: String): String? = null.also { keys += key }

            override fun write(key: String, value: String?) {
                keys += key
            }
        }
        val viewModel = FormViewModel(testDispatcher(), store)

        viewModel.dispatch(FormAction.NameWritten("Ana"))
        runCurrent()

        assertTrue(store.keys.isEmpty())
        viewModel.onCreated(null)
        runCurrent()
        assertEquals(listOf("name", "age", "plan", "name", "age", "plan"), store.keys)
        viewModel.close()
    }

    @Test
    fun `a key can only be added once to a backup`() {
        assertFailsWith<IllegalArgumentException> {
            backupOf<FormState>(MemoryStore(), restoreOn = Opened::class) {
                add("name", save = { name }, restore = { copy(name = it) })
                add("name", save = { name }, restore = { copy(name = it) })
            }
        }
    }

    @Test
    fun `the backups of a source can't repeat keys in the same store`() = runTest {
        val store = MemoryStore()

        assertFailsWith<IllegalArgumentException> {
            EmaSource<FormState, FormAction, EmaEvent.EMPTY>(FormState(), configurationOn(testDispatcher())) {
                plugins {
                    +nameBackup(store)
                    +nameBackup(store)
                }
            }
        }
    }

    @Test
    fun `the backups of a source can repeat keys in different stores`() = runTest {
        val source = EmaSource<FormState, FormAction, EmaEvent.EMPTY>(FormState(), configurationOn(testDispatcher())) {
            plugins {
                +nameBackup(MemoryStore())
                +nameBackup(MemoryStore())
            }
        }

        source.close()
    }

    @Test
    fun `a backup can only be installed in one source`() = runTest {
        val backup = nameBackup(MemoryStore())
        formSource(testDispatcher(), backup).close()

        assertFailsWith<IllegalStateException> { formSource(testDispatcher(), backup) }
    }

    @Test
    fun `a backup that isn't installed doesn't restore anything`() = runTest {
        val source = EmaSource<FormState, FormAction, EmaEvent.EMPTY>(FormState(), configurationOn(testDispatcher())) {}

        source.dispatchAction(EmaRestoreState(mapOf("name" to "Ana")))
        runCurrent()

        assertEquals(FormState(), source.state)
        source.close()
    }

    @Test
    fun `a source on its own saves and restores its values after the action set`() = runTest {
        val store = MemoryStore()
        val first = formSource(testDispatcher(), nameBackup(store))
        first.dispatchAction(Opened)
        first.dispatch(FormAction.NameWritten("Ana"))
        runCurrent()
        first.close()

        val second = formSource(testDispatcher(), nameBackup(store))
        second.dispatchAction(Opened)
        runCurrent()

        assertEquals(mapOf("name" to "Ana"), store.values)
        assertEquals(FormState(name = "Ana"), second.state)
        second.close()
    }

    @Test
    fun `a source doesn't save its values before the action set`() = runTest {
        val store = MemoryStore()
        val source = formSource(testDispatcher(), nameBackup(store))

        source.dispatch(FormAction.NameWritten("Ana"))
        runCurrent()

        assertTrue(store.writes.isEmpty())
        source.close()
    }

    @Test
    fun `the values are restored only once`() = runTest {
        val store = MemoryStore(mapOf("name" to "Ana"))
        val source = formSource(testDispatcher(), nameBackup(store))
        source.dispatchAction(Opened)
        source.dispatch(FormAction.NameWritten("Luis"))

        source.dispatchAction(Opened)
        runCurrent()

        assertEquals(FormState(name = "Luis"), source.state)
        source.close()
    }

    @Test
    fun `a source restores the values of the action without a store`() = runTest {
        val source = formSource(testDispatcher(), nameBackup(EmaStateStore.None))

        source.dispatchAction(EmaRestoreState(mapOf("name" to "Ana", "unknown" to "value")))
        runCurrent()

        assertEquals(FormState(name = "Ana"), source.state)
        source.close()
    }

    @Test
    fun `a backup installed before a middleware that delays an action saves its new state`() = runTest {
        val store = MemoryStore()
        val source = EmaSource<FormState, FormAction, EmaEvent.EMPTY>(FormState(), configurationOn(testDispatcher())) {
            plugins { +nameBackup(store) }
            middlewares {
                add<FormAction.NameWritten> { nextAfter { delay(300.milliseconds) } }
            }
            reducers {
                add<FormAction.NameWritten> { action -> state.copy(name = action.name) }
            }
        }
        source.dispatchAction(Opened)

        source.dispatch(FormAction.NameWritten("Ana"))
        advanceTimeBy(299.milliseconds)
        assertTrue(store.values.isEmpty())
        advanceTimeBy(2.milliseconds)

        assertEquals(mapOf("name" to "Ana"), store.values)
        source.close()
    }

    @Test
    fun `the values aren't restored if a middleware stops the action that restores them`() = runTest {
        val store = MemoryStore(mapOf("name" to "Ana"))
        val source = EmaSource<FormState, FormAction, EmaEvent.EMPTY>(FormState(), configurationOn(testDispatcher())) {
            plugins { +nameBackup(store) }
            middlewares {
                add<Opened> { }
            }
        }

        source.dispatchAction(Opened)
        runCurrent()

        assertEquals(FormState(), source.state)
        assertEquals(mapOf("name" to "Ana"), store.values)
        assertTrue(store.writes.isEmpty())
        source.close()
    }

    @Test
    fun `the values not restored because a middleware stops the restore are saved from the state`() = runTest {
        val store = MemoryStore(mapOf("name" to "Ana"))
        val source = EmaSource<FormState, FormAction, EmaEvent.EMPTY>(FormState(), configurationOn(testDispatcher())) {
            plugins { +nameBackup(store) }
            middlewares {
                add<EmaRestoreState> { }
            }
            reducers {
                add<FormAction.NameWritten> { action -> state.copy(name = action.name) }
            }
        }
        source.dispatchAction(Opened)
        runCurrent()
        assertEquals(FormState(), source.state)
        assertTrue(store.values.isEmpty())
        source.dispatch(FormAction.NameWritten("Luis"))
        runCurrent()

        assertEquals(FormState(name = "Luis"), source.state)
        assertEquals(mapOf("name" to "Luis"), store.values)
        source.close()
    }

    @Test
    fun `an action queued before the restore doesn't replace the values that are going to be restored`() = runTest {
        val store = MemoryStore(mapOf("name" to "Ana"))
        val source = EmaSource<FormState, FormAction, EmaEvent.EMPTY>(
            FormState(),
            configurationOn(StandardTestDispatcher(testScheduler))
        ) {
            plugins { +nameBackup(store) }
        }

        // Queued before the restore, which is dispatched when Opened is processed
        source.dispatchAction(Opened)
        source.dispatch(FormAction.AgeWritten(30))
        runCurrent()

        assertEquals(FormState(name = "Ana"), source.state)
        assertTrue(store.writes.isEmpty())
        source.close()
    }

    private fun formSource(dispatcher: CoroutineDispatcher, backup: EmaBackup<FormState>) =
        EmaSource<FormState, FormAction, EmaEvent.EMPTY>(FormState(), configurationOn(dispatcher)) {
            plugins { +backup }
            reducers {
                add<FormAction.NameWritten> { action -> state.copy(name = action.name) }
            }
        }
}
