import com.carmabs.emanate.core.initializer.EmaInitializer
import com.carmabs.emanate.core.initializer.EmaInitializerSerializer
import com.carmabs.emanate.core.navigator.EmaNavigator
import com.carmabs.emanate.core.view.EmaView
import com.carmabs.emanate.core.view.EmaViewModelTrigger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

class FakeNavigator : EmaNavigator<CounterEvent> {
    val navigated = mutableListOf<CounterEvent>()
    var backResult: Any? = null
    override fun navigate(event: CounterEvent) {
        navigated.add(event)
    }

    override fun navigateBack(result: Any?): Boolean {
        backResult = result
        return true
    }
}

class FakeSerializer(private val initializer: EmaInitializer?) : EmaInitializerSerializer {
    override fun save(initializer: EmaInitializer) = Unit
    override fun restore(): EmaInitializer? = initializer
}

class FakeView(
    override val coroutineScope: CoroutineScope,
    override val viewModel: CounterViewModel,
    override val navigator: EmaNavigator<CounterEvent>? = null,
    override val startTrigger: EmaViewModelTrigger? = null,
    override val initializerSerializer: EmaInitializerSerializer? = null
) : EmaView<CounterState, CounterViewModel, CounterEvent> {

    override var previousState: CounterState? = null
    val states = mutableListOf<CounterState>()
    val events = mutableListOf<CounterEvent>()
    var backResult: Any? = "none"

    override fun onState(state: CounterState) {
        states.add(state)
    }

    override suspend fun onEvent(event: CounterEvent) {
        events.add(event)
    }

    override fun onBack(result: Any?): Boolean {
        backResult = result
        return false
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class EmaViewTest {

    private fun TestScope.scope() = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

    @Test
    fun `binding renders every state and remembers the previous one`() = runTest {
        val viewModel = CounterViewModel(UnconfinedTestDispatcher(testScheduler))
        val view = FakeView(scope(), viewModel)
        val jobs = view.onBindView(view.coroutineScope, viewModel)

        viewModel.dispatch(CounterAction.Increment)

        assertEquals(listOf(CounterState(), CounterState(count = 1)), view.states)
        assertEquals(CounterState(count = 1), view.previousState)
        view.onUnbindView(jobs, viewModel)
    }

    @Test
    fun `events are delivered once and consumed`() = runTest {
        val viewModel = CounterViewModel(UnconfinedTestDispatcher(testScheduler))
        val view = FakeView(scope(), viewModel)
        val jobs = view.onBindView(view.coroutineScope, viewModel)

        viewModel.message()
        viewModel.message(allowDuplicated = true)

        assertEquals(listOf<CounterEvent>(CounterEvent.Message, CounterEvent.Message), view.events)
        view.onUnbindView(jobs, viewModel)
        val newView = FakeView(scope(), viewModel)
        newView.onBindView(newView.coroutineScope, viewModel)
        assertTrue(newView.events.isEmpty())
    }

    @Test
    fun `an event equal to a consumed one is delivered again`() = runTest {
        val viewModel = CounterViewModel(UnconfinedTestDispatcher(testScheduler))
        val view = FakeView(scope(), viewModel)
        view.onBindView(view.coroutineScope, viewModel)

        viewModel.message()
        viewModel.message()

        assertEquals(listOf<CounterEvent>(CounterEvent.Message, CounterEvent.Message), view.events)
    }

    @Test
    fun `events published while unbound are delivered when the view binds`() = runTest {
        val viewModel = CounterViewModel(UnconfinedTestDispatcher(testScheduler))
        viewModel.message()
        val view = FakeView(scope(), viewModel)
        view.onBindView(view.coroutineScope, viewModel)
        assertEquals(listOf<CounterEvent>(CounterEvent.Message), view.events)
    }

    @Test
    fun `unbinding cancels the jobs and stops the ViewModel`() = runTest {
        val viewModel = CounterViewModel(UnconfinedTestDispatcher(testScheduler))
        val view = FakeView(scope(), viewModel)
        val jobs = view.onBindView(view.coroutineScope, viewModel)
        val boundJobs = jobs.toList()

        view.onUnbindView(jobs, viewModel)
        viewModel.dispatch(CounterAction.Increment)

        assertTrue(jobs.isEmpty())
        assertTrue(boundJobs.all { it.isCancelled })
        assertEquals(1, view.states.size)
        assertEquals(listOf("stopped"), viewModel.hooks)
    }

    @Test
    fun `lifecycle calls reach the ViewModel with the restored initializer`() = runTest {
        val viewModel = CounterViewModel(UnconfinedTestDispatcher(testScheduler))
        val view = FakeView(scope(), viewModel, initializerSerializer = FakeSerializer(CounterInitializer(3)))

        view.onCreate(viewModel)
        view.onStartView(viewModel)
        view.onResumeView(viewModel)
        view.onPauseView(viewModel)

        assertEquals(listOf("created", "broadcast", "started", "resumed", "paused"), viewModel.hooks)
        assertEquals(3, viewModel.stateFlow.value.count)
    }

    @Test
    fun `with a start trigger the ViewModel waits until it is started`() = runTest {
        val viewModel = CounterViewModel(UnconfinedTestDispatcher(testScheduler))
        val trigger = EmaViewModelTrigger()
        val view = FakeView(scope(), viewModel, startTrigger = trigger)

        view.onCreate(viewModel)
        view.onStartView(viewModel)
        view.onResumeView(viewModel)
        assertTrue(viewModel.hooks.isEmpty())
        assertFalse(trigger.hasBeenStarted)

        trigger.startViewModel()
        assertTrue(trigger.hasBeenStarted)
        assertEquals(listOf("created", "broadcast", "started"), viewModel.hooks)

        view.onStartView(viewModel)
        view.onResumeView(viewModel)
        assertEquals(listOf("created", "broadcast", "started", "started", "resumed"), viewModel.hooks)
    }

    @Test
    fun `navigation goes through the navigator`() = runTest {
        val navigator = FakeNavigator()
        val viewModel = CounterViewModel(UnconfinedTestDispatcher(testScheduler))
        val view = FakeView(scope(), viewModel, navigator = navigator)

        view.navigate(CounterEvent.Message)
        assertTrue(view.navigateBack("result"))

        assertEquals(listOf<CounterEvent>(CounterEvent.Message), navigator.navigated)
        assertEquals("result", navigator.backResult)
        assertEquals("none", view.backResult)
    }

    @Test
    fun `without navigator navigate fails and navigateBack uses onBack`() = runTest {
        val view = FakeView(scope(), CounterViewModel(UnconfinedTestDispatcher(testScheduler)))
        assertFailsWith<RuntimeException> { view.navigate(CounterEvent.Message) }
        assertFalse(view.navigateBack("result"))
        assertEquals("result", view.backResult)
    }
}
