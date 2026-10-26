import com.carmabs.emanate.core.Emanate
import com.carmabs.emanate.core.action.EmaAction
import com.carmabs.emanate.core.broadcast.EmaActionBroadcaster
import com.carmabs.emanate.core.configurationOn
import com.carmabs.emanate.core.error.EmaErrorPolicy
import com.carmabs.emanate.core.initializer.EmaInitializer
import com.carmabs.emanate.core.logging.EmaLogger
import com.carmabs.emanate.core.model.EmaConfiguration
import com.carmabs.emanate.core.model.reflection.Reflection
import com.carmabs.emanate.core.sideeffect.SideEffectOutput
import com.carmabs.emanate.core.sideeffect.EmaSideEffectListener
import com.carmabs.emanate.core.state.EmaEvent
import com.carmabs.emanate.core.state.EmaState
import com.carmabs.emanate.core.viewmodel.EmaViewModelAction
import com.carmabs.emanate.core.viewmodel.EmaViewModelClassic
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull

data class CounterState(val count: Int = 0, val text: String = "") : EmaState

sealed interface CounterAction : EmaAction {
    data object Increment : CounterAction
    data class Write(val text: String) : CounterAction
}

sealed interface CounterEvent : EmaEvent {
    data class LimitReached(val limit: Int) : CounterEvent
    data object Message : CounterEvent
}

data class CounterInitializer(val start: Int) : EmaInitializer

class CounterViewModel(dispatcher: CoroutineDispatcher, private val renderOnInitialization: Boolean = true) :
    EmaViewModelAction<CounterState, CounterAction, CounterEvent>(CounterState(), configurationOn(dispatcher)) {

    val hooks = mutableListOf<String>()

    override val updateOnInitialization: Boolean
        get() = renderOnInitialization

    override fun onStateCreated(initializer: EmaInitializer?) {
        hooks.add("created")
        (initializer as? CounterInitializer)?.also { updateState { copy(count = it.start) } }
    }

    override fun onBroadcastListenerSetup() {
        hooks.add("broadcast")
    }

    override fun onViewStarted() {
        hooks.add("started")
    }

    override fun onViewResumed() {
        hooks.add("resumed")
    }

    override fun onViewPaused() {
        hooks.add("paused")
    }

    override fun onViewStopped() {
        hooks.add("stopped")
    }

    override fun onDestroy() {
        hooks.add("destroyed")
    }

    override fun onAction(action: CounterAction) {
        when (action) {
            CounterAction.Increment -> {
                updateState { copy(count = count + 1) }
                if (state.count == LIMIT) postEvent(CounterEvent.LimitReached(LIMIT))
            }

            is CounterAction.Write -> updateState { copy(text = action.text) }
        }
    }

    fun message(allowDuplicated: Boolean = false) = postEvent(CounterEvent.Message, allowDuplicated)

    fun <T> launch(
        logName: String? = null,
        context: CoroutineContext = scope.coroutineContext,
        block: suspend CoroutineScope.() -> T
    ) = sideEffect(dispatcher = context, logName = logName, action = block)

    fun <T> launchSingle(id: String, block: suspend CoroutineScope.() -> T) = singleSideEffect(id, action = block)

    fun currentScope() = scope

    companion object {
        const val LIMIT = 2
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class EmaViewModelActionTest {

    @AfterTest
    fun tearDown() {
        Emanate.reset()
    }

    private fun TestScope.createViewModel(renderOnInitialization: Boolean = true) =
        CounterViewModel(UnconfinedTestDispatcher(testScheduler), renderOnInitialization)

    @Test
    fun `initial state is exposed before any update`() = runTest {
        val viewModel = createViewModel()
        assertEquals(CounterState(), viewModel.initialState)
        assertEquals(CounterState(), viewModel.stateFlow.value)
        assertTrue(viewModel.shouldRenderState)
    }

    @Test
    fun `actions update the state`() = runTest {
        val viewModel = createViewModel()
        viewModel.dispatch(CounterAction.Increment)
        viewModel.dispatch(CounterAction.Write("hello"))
        assertEquals(CounterState(count = 1, text = "hello"), viewModel.stateFlow.value)
    }

    @Test
    fun `onCreated runs the creation hooks once with the initializer`() = runTest {
        val viewModel = createViewModel()
        viewModel.onCreated(CounterInitializer(start = 5))
        viewModel.onCreated(CounterInitializer(start = 9))
        assertEquals(listOf("created", "broadcast"), viewModel.hooks)
        assertEquals(5, viewModel.stateFlow.value.count)
    }

    @Test
    fun `view lifecycle calls the matching hooks`() = runTest {
        val viewModel = createViewModel()
        viewModel.onStartView()
        viewModel.onResumeView()
        viewModel.onPauseView()
        viewModel.onStopView()
        assertEquals(listOf("started", "resumed", "paused", "stopped"), viewModel.hooks)
    }

    @Test
    fun `the state is not rendered before the first update when updateOnInitialization is false`() = runTest {
        val viewModel = createViewModel(renderOnInitialization = false)
        assertFalse(viewModel.shouldRenderState)
        viewModel.dispatch(CounterAction.Increment)
        assertTrue(viewModel.shouldRenderState)
    }

    @Test
    fun `events are queued until they are consumed`() = runTest {
        val viewModel = createViewModel()
        viewModel.dispatch(CounterAction.Increment)
        viewModel.dispatch(CounterAction.Increment)
        val event = CounterEvent.LimitReached(CounterViewModel.LIMIT)
        assertEquals(listOf<CounterEvent>(event), viewModel.eventFlow.first())

        viewModel.consumeEvent(event)
        assertNull(withTimeoutOrNull(100) { viewModel.eventFlow.first() })
    }

    @Test
    fun `equal pending events are not duplicated unless allowed`() = runTest {
        val viewModel = createViewModel()
        viewModel.message()
        viewModel.message()
        assertEquals(1, viewModel.eventFlow.first().size)
        viewModel.message(allowDuplicated = true)
        assertEquals(2, viewModel.eventFlow.first().size)
    }

    @Test
    fun `sideEffect reports success and finish`() = runTest {
        val viewModel = createViewModel()
        var success: Int? = null
        var finished = false
        viewModel.launch { 42 }
            .onSuccess { success = it }
            .onError { error("Unexpected error") }
            .onFinish { finished = true }
        assertEquals(42, success)
        assertTrue(finished)
    }

    @Test
    fun `sideEffect reports errors when exceptions are caught`() = runTest {
        val viewModel = createViewModel()
        var failure: Throwable? = null
        var finished = false
        viewModel.launch<Int> { throw IllegalStateException("boom") }
            .onError { failure = it }
            .onFinish { finished = true }
        assertIs<IllegalStateException>(failure)
        assertTrue(finished)
    }

    @Test
    fun `listeners added before the work ends are called when it ends`() = runTest {
        val viewModel = CounterViewModel(StandardTestDispatcher(testScheduler))
        var success: Int? = null
        var failure: Throwable? = null
        var finished = false
        val handler = viewModel.launch {
            delay(100)
            1
        }
        handler.onSuccess { success = it }.onFinish { finished = true }
        viewModel.launch<Int> {
            delay(100)
            error("boom")
        }.onError { failure = it }
        assertNull(success)
        advanceUntilIdle()
        assertEquals(1, success)
        assertTrue(finished)
        assertIs<IllegalStateException>(failure)
    }

    @Test
    fun `singleSideEffect cancels the previous work with the same id without reporting an error`() = runTest {
        val viewModel = CounterViewModel(StandardTestDispatcher(testScheduler))
        val results = mutableListOf<String>()
        var errors = 0
        viewModel.launchSingle("search") {
            delay(100)
            "first"
        }
            .onSuccess { results.add(it) }.onError { errors++ }
        viewModel.launchSingle("search") {
            delay(100)
            "second"
        }
            .onSuccess { results.add(it) }.onError { errors++ }
        advanceUntilIdle()
        assertEquals(listOf("second"), results)
        assertEquals(0, errors)
    }

    @Test
    fun `close cancels the scope and calls onDestroy`() = runTest {
        val viewModel = createViewModel()
        viewModel.close()
        assertFalse(viewModel.currentScope().coroutineContext[kotlinx.coroutines.Job]!!.isActive)
        assertEquals(listOf("destroyed"), viewModel.hooks)
    }

    @Test
    fun `the id identifies the ViewModel class`() = runTest {
        assertEquals("EmaViewModel ID: CounterViewModel", createViewModel().id)
    }

    @Test
    fun `the side effect listener and the errors receive the reflection with the name`() = runTest {
        val successes = mutableListOf<Reflection>()
        val finishes = mutableListOf<Reflection>()
        val errors = mutableListOf<Throwable>()
        val logs = mutableListOf<String>()
        Emanate.init(
            EmaConfiguration(
                sideEffectListener = object : EmaSideEffectListener {
                    override fun onSuccess(reflection: Reflection, output: SideEffectOutput, duration: Duration) {
                        successes.add(reflection)
                    }

                    override fun onFinish(reflection: Reflection, duration: Duration) {
                        finishes.add(reflection)
                    }
                },
                errorPolicy = EmaErrorPolicy.Catch { errors.add(it.exception) },
                logger = EmaLogger { tag, message -> logs += "$tag: $message" }
            )
        )
        val viewModel = createViewModel()

        viewModel.launch { 1 }
        viewModel.launch(logName = "login") { 1 }
        viewModel.launch<Int> { error("boom") }

        assertEquals(listOf("sideEffect", "login"), successes.map { it.name })
        assertEquals(listOf("sideEffect", "login", "sideEffect"), finishes.map { it.name })
        assertEquals("CounterViewModel", successes.first().containerClassName)
        assertEquals("CounterViewModel", successes.first().containerClassQualifiedName)
        assertEquals(1, errors.size)
        // The caught errors are also logged
        assertTrue(logs.any { it.startsWith("CounterViewModel: sideEffect failed: ") && "boom" in it })
    }

    @Test
    fun `the default method name names the container`() = runTest {
        var name: String? = null
        Emanate.init(
            EmaConfiguration(
                sideEffectListener = object : EmaSideEffectListener {
                    override fun onFinish(reflection: Reflection, duration: Duration) {
                        name = reflection.name
                    }
                }
            )
        )
        createViewModel().launch { 1 }
        assertEquals("sideEffect", name)
    }

    @Test
    fun `exceptions are rethrown with the Throw policy`() = runTest {
        Emanate.init(EmaConfiguration(errorPolicy = EmaErrorPolicy.Throw))
        var uncaught: Throwable? = null
        val viewModel = CounterViewModel(UnconfinedTestDispatcher(testScheduler))
        val handler = CoroutineExceptionHandler { _, e -> uncaught = e }
        viewModel.launch<Int>(context = viewModel.currentScope().coroutineContext + handler) { error("boom") }
        assertIs<IllegalStateException>(uncaught)
    }
}

data class UserSent(val user: String) : EmaAction

class SenderViewModel(dispatcher: CoroutineDispatcher) :
    EmaViewModelClassic<CounterState, CounterEvent>(CounterState(), configurationOn(dispatcher)) {
    override fun onStateCreated(initializer: EmaInitializer?) = Unit
    fun send(user: String, keepUntilReceived: Boolean = false) = broadcast(UserSent(user), keepUntilReceived)
}

class ReceiverViewModel(dispatcher: CoroutineDispatcher) :
    EmaViewModelClassic<CounterState, CounterEvent>(CounterState(), configurationOn(dispatcher)) {
    val received = mutableListOf<String>()
    override fun onStateCreated(initializer: EmaInitializer?) = Unit
    override fun onBroadcastListenerSetup() {
        registerBroadcastListener<UserSent> { received.add(it.user) }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class EmaClassicBroadcastTest {

    private fun TestScope.newDispatcher() = UnconfinedTestDispatcher(testScheduler)

    @AfterTest
    fun checkBroadcasterIsClean() {
        assertEquals(0, EmaActionBroadcaster.Shared.listenerCount)
        assertEquals(0, EmaActionBroadcaster.Shared.pendingCount)
    }

    @Test
    fun `the action is delivered at the moment to every receiver`() = runTest {
        val firstReceiver = ReceiverViewModel(newDispatcher()).apply { onCreated() }
        val secondReceiver = ReceiverViewModel(newDispatcher()).apply { onCreated() }

        SenderViewModel(newDispatcher()).send("Ana")

        assertEquals(listOf("Ana"), firstReceiver.received)
        assertEquals(listOf("Ana"), secondReceiver.received)
        firstReceiver.close()
        secondReceiver.close()
    }

    @Test
    fun `an action without receiver is lost`() = runTest {
        SenderViewModel(newDispatcher()).send("Ana")

        val receiver = ReceiverViewModel(newDispatcher()).apply { onCreated() }

        assertTrue(receiver.received.isEmpty())
        receiver.close()
    }

    @Test
    fun `a kept action is delivered only to the first receiver that registers`() = runTest {
        SenderViewModel(newDispatcher()).send("Ana", keepUntilReceived = true)
        assertEquals(1, EmaActionBroadcaster.Shared.pendingCount)

        val firstReceiver = ReceiverViewModel(newDispatcher()).apply { onCreated() }
        val secondReceiver = ReceiverViewModel(newDispatcher()).apply { onCreated() }

        assertEquals(listOf("Ana"), firstReceiver.received)
        assertTrue(secondReceiver.received.isEmpty())
        firstReceiver.close()
        secondReceiver.close()
    }

    @Test
    fun `only the last kept action of each class is delivered`() = runTest {
        val sender = SenderViewModel(newDispatcher())
        sender.send("Ana", keepUntilReceived = true)
        sender.send("Luis", keepUntilReceived = true)

        val receiver = ReceiverViewModel(newDispatcher()).apply { onCreated() }

        assertEquals(listOf("Luis"), receiver.received)
        receiver.close()
    }

    @Test
    fun `the receiver stops listening when it is cleared`() = runTest {
        val receiver = ReceiverViewModel(newDispatcher()).apply { onCreated() }
        receiver.close()

        SenderViewModel(newDispatcher()).send("Ana")

        assertTrue(receiver.received.isEmpty())
    }
}
