import com.carmabs.emanate.core.Emanate
import com.carmabs.emanate.core.action.DefaultEmaActionDispatcher
import com.carmabs.emanate.core.action.EmaAction
import com.carmabs.emanate.core.action.EmaActionDispatcher
import com.carmabs.emanate.core.concurrency.EmaDispatchers
import com.carmabs.emanate.core.delegate.emaBooleanDelegate
import com.carmabs.emanate.core.delegate.emaStartTriggerDelegate
import com.carmabs.emanate.core.delegate.emaSyncDelegate
import com.carmabs.emanate.core.dialog.EmaDialogListener
import com.carmabs.emanate.core.extension.concat
import com.carmabs.emanate.core.extension.until
import com.carmabs.emanate.core.extension.untilInstanceOf
import com.carmabs.emanate.core.extension.whileIsInstanceOf
import com.carmabs.emanate.core.initializer.EmaInitializer
import com.carmabs.emanate.core.logging.EmaDataClassPrinter
import com.carmabs.emanate.core.logging.toStringPretty
import com.carmabs.emanate.core.manager.PermissionState
import com.carmabs.emanate.core.model.EmaConfiguration
import com.carmabs.emanate.core.model.EmaImage
import com.carmabs.emanate.core.model.EmaMultiplePermissionRequest
import com.carmabs.emanate.core.model.EmaPermissionRequest
import com.carmabs.emanate.core.model.EmaResult
import com.carmabs.emanate.core.model.flatMap
import com.carmabs.emanate.core.model.flatMapError
import com.carmabs.emanate.core.model.map
import com.carmabs.emanate.core.model.mapError
import com.carmabs.emanate.core.model.mapResult
import com.carmabs.emanate.core.model.onFailure
import com.carmabs.emanate.core.model.onSuccess
import com.carmabs.emanate.core.navigator.EmaNavigationNode
import com.carmabs.emanate.core.selector.EmaUniqueSelector
import com.carmabs.emanate.core.state.EmaEvent
import com.carmabs.emanate.core.state.EmaState
import com.carmabs.emanate.core.usecase.EmaFlowUseCase
import com.carmabs.emanate.core.usecase.EmaSyncUseCase
import com.carmabs.emanate.core.usecase.EmaUseCase
import com.carmabs.emanate.core.utils.EmaUtils
import com.carmabs.emanate.core.value.EmaUriRes
import com.carmabs.emanate.core.value.EmaUriType
import com.carmabs.emanate.core.viewmodel.EmaLifecycle
import com.carmabs.emanate.core.viewmodel.EmaViewModelClassic
import com.carmabs.emanate.core.viewmodel.EmaViewModelContract
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.resume
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

class EmaResultTest {

    private val success: EmaResult<Int, String> = EmaResult.success(2)
    private val failure: EmaResult<Int, String> = EmaResult.failure("error")

    @Test
    fun `reading values`() {
        assertTrue(success.isSuccess)
        assertFalse(success.isFailure)
        assertEquals(2, success.getOrNull())
        assertEquals(2, success.getOrDefault(0))
        assertEquals(2, success.getOrThrow())
        assertNull(success.getFailureOrNull())

        assertTrue(failure.isFailure)
        assertNull(failure.getOrNull())
        assertEquals(0, failure.getOrDefault(0))
        assertEquals("error", failure.getFailureOrNull())
        assertFailsWith<IllegalArgumentException> { failure.getOrThrow() }
        assertFailsWith<IllegalStateException> {
            EmaResult.failure<Int, Throwable>(IllegalStateException()).getOrThrow()
        }
    }

    @Test
    fun `transformations`() {
        assertEquals(4, success.map { it * 2 }.getOrNull())
        assertEquals("error", failure.map { it * 2 }.getFailureOrNull())
        assertEquals(6, success.flatMap { EmaResult.success<Int, String>(it * 3) }.getOrNull())
        assertEquals("error", failure.flatMap { EmaResult.success<Int, String>(it) }.getFailureOrNull())
        assertEquals(5, failure.mapError { it.length }.getFailureOrNull())
        assertEquals(2, success.mapError { it.length }.getOrNull())
        assertEquals(1, failure.flatMapError { EmaResult.success<Int, Int>(1) }.getOrNull())
        assertEquals(2, success.flatMapError { EmaResult.failure<Int, Int>(1) }.getOrNull())
        assertEquals(
            "2",
            success.mapResult {
                EmaResult.success<String, String>(it.getOrNull().toString())
            }.getOrNull()
        )
    }

    @Test
    fun `side effects return the same result`() {
        var value = 0
        var error = ""
        assertSame(success, success.onSuccess { value = it }.onFailure { error = it })
        failure.onSuccess { value = -1 }.onFailure { error = it }
        assertEquals(2, value)
        assertEquals("error", error)
    }
}

class EmaModelTest {

    @AfterTest
    fun tearDown() {
        Emanate.reset()
    }

    @Test
    fun `Emanate uses the default configuration until it is initialized once`() {
        assertSame(Emanate.configuration, Emanate.configuration)
        assertSame(EmaDataClassPrinter.ToString, Emanate.configuration.printer)
        val configuration = EmaConfiguration(printer = { "custom" })
        Emanate.init(configuration)
        assertSame(configuration, Emanate.configuration)
        assertFailsWith<IllegalStateException> { Emanate.init(EmaConfiguration()) }
    }

    @Test
    fun `tests can replace the configuration any number of times`() {
        Emanate.init(EmaConfiguration())
        val first = EmaConfiguration(printer = { "first" })
        val second = EmaConfiguration(printer = { "second" })

        Emanate.initForTest(first)
        assertSame(first, Emanate.configuration)
        Emanate.initForTest(second)
        assertSame(second, Emanate.configuration)
    }

    @Test
    fun `ViewModels and use cases run on the dispatchers of the configuration`() = runTest {
        val main = UnconfinedTestDispatcher(testScheduler)
        val io = StandardTestDispatcher(testScheduler)
        Emanate.init(EmaConfiguration(dispatchers = EmaDispatchers(main = main, io = io)))
        val useCase = object : EmaUseCase<Unit, ContinuationInterceptor?>() {
            override suspend fun useCaseFunction(input: Unit) = currentCoroutineContext()[ContinuationInterceptor]
        }

        val viewModel = object : EmaViewModelClassic<EmaState.EMPTY, EmaEvent.EMPTY>(EmaState.EMPTY) {
            val interceptor = scope.coroutineContext[ContinuationInterceptor]
            override fun onStateCreated(initializer: EmaInitializer?) = Unit
        }

        assertSame(main, viewModel.interceptor)
        assertSame(io, useCase(Unit))
    }

    @Test
    fun `use cases run on the dispatcher they receive instead of the one of the configuration`() = runTest {
        val io = StandardTestDispatcher(testScheduler)
        val useCase = object : EmaUseCase<Unit, ContinuationInterceptor?>(io) {
            override suspend fun useCaseFunction(input: Unit) = currentCoroutineContext()[ContinuationInterceptor]
        }
        val flowUseCase = object : EmaFlowUseCase<Unit, ContinuationInterceptor?>(io) {
            override fun useCaseFunction(input: Unit) =
                flow { emit(currentCoroutineContext()[ContinuationInterceptor]) }
        }

        assertSame(io, useCase(Unit))
        assertSame(io, flowUseCase(Unit).first())
    }

    @Test
    fun `the default dispatchers can be created on any platform`() {
        val dispatchers = EmaDispatchers()

        assertSame(Dispatchers.Main, dispatchers.main)
        assertSame(Dispatchers.Default, dispatchers.default)
    }

    @Test
    fun `objects are printed with the configured printer`() {
        assertEquals("CounterState(count=1, text=)", CounterState(count = 1).toStringPretty())
        assertEquals("null", EmaDataClassPrinter.ToString.print(null))
        Emanate.init(EmaConfiguration(printer = { "printed $it" }))
        assertEquals("printed 1", 1.toStringPretty())
        assertEquals("passed 1", 1.toStringPretty(EmaDataClassPrinter { "passed $it" }))
    }

    @Test
    fun `action types and empty values`() {
        assertEquals("EmaAction", EmaAction.type)
        assertEquals("EmaAction", CounterAction.Increment.type)
        assertEquals("Initializer", CounterInitializer(1).type)
        assertEquals("Lifecycle", EmaLifecycle.Started.type)
        assertEquals("EmaAction", EmaAction.EMPTY.type)
        assertEquals("EmaInitializer", EmaInitializer.KEY)
        assertEquals("Initializer", EmaInitializer.EMPTY.type)
        assertTrue(EmaState.EMPTY is EmaState)
        assertTrue(EmaEvent.EMPTY is EmaEvent)
    }

    @Test
    fun `action dispatchers`() {
        val received = mutableListOf<CounterAction>()
        DefaultEmaActionDispatcher<CounterAction> { received.add(it) }.dispatch(CounterAction.Increment)
        EmaActionDispatcher.EMPTY.dispatch(CounterAction.Increment)
        assertEquals(listOf<CounterAction>(CounterAction.Increment), received)
    }

    @Test
    fun `empty ViewModel does nothing`() = runTest {
        val viewModel = EmaViewModelContract.EMPTY
        viewModel.onCreated()
        viewModel.onStartView()
        viewModel.onResumeView()
        viewModel.onPauseView()
        viewModel.onStopView()
        viewModel.close()
        viewModel.consumeEvent(EmaEvent.EMPTY)
        assertEquals(EmaState.EMPTY, viewModel.initialState)
        assertEquals(EmaState.EMPTY, viewModel.stateFlow.value)
        assertTrue(viewModel.shouldRenderState)
        assertEquals(emptyList(), viewModel.eventFlow.toList())
    }

    @Test
    fun `images compare their content`() {
        assertEquals(EmaImage.ByteArray(byteArrayOf(1, 2)), EmaImage.ByteArray(byteArrayOf(1, 2)))
        assertEquals(EmaImage.ByteArray(byteArrayOf(1, 2)).hashCode(), EmaImage.ByteArray(byteArrayOf(1, 2)).hashCode())
        assertNotEquals(EmaImage.ByteArray(byteArrayOf(1)), EmaImage.ByteArray(byteArrayOf(2)))
        assertNotEquals<Any>(EmaImage.ByteArray(byteArrayOf(1)), "image")
        assertEquals(EmaImage.Id(1, width = 2), EmaImage.Id(1, width = 2))
        val uri = EmaImage.Uri(EmaUriRes("a", EmaUriType.Drawable), height = 3, colorTint = 4)
        assertEquals(3, uri.height)
        assertEquals(4, uri.colorTint)
        assertNull(uri.width)
    }

    @Test
    fun `permission requests`() {
        var single: PermissionState? = null
        var multiple: Map<String, PermissionState>? = null
        val request = EmaPermissionRequest.createRequest { single = it }
        val multipleRequest = EmaMultiplePermissionRequest.createRequest { multiple = it }
        assertTrue(request.shouldRequest)
        assertTrue(multipleRequest.shouldRequest)
        request.onPermissionResponse(PermissionState.GRANTED)
        multipleRequest.onPermissionResponse(mapOf("a" to PermissionState.NOT_GRANTED))
        assertEquals(PermissionState.GRANTED, single)
        assertEquals(mapOf("a" to PermissionState.NOT_GRANTED), multiple)

        assertFalse(EmaPermissionRequest.cancelRequest().shouldRequest)
        assertFalse(EmaMultiplePermissionRequest.cancelRequest().shouldRequest)
        EmaPermissionRequest.cancelRequest().onPermissionResponse(PermissionState.GRANTED)
        EmaMultiplePermissionRequest.cancelRequest().onPermissionResponse(emptyMap())
    }

    @Test
    fun `dialog listener outside press defaults to back`() {
        var backs = 0
        val listener = object : EmaDialogListener {
            override fun onBackPressed() {
                backs++
            }
        }
        listener.onOutsidePressed()
        listener.onDestroyed()
        assertEquals(1, backs)
    }

    @Test
    fun `unique selector selects one option and deselects the rest`() {
        val selected = mutableListOf<String>()
        val unselected = mutableListOf<String>()
        EmaUniqueSelector(
            listOf("a", "b", "c"),
            object : EmaUniqueSelector.OnSelectionListener<String> {
                override fun onSelected(option: String) {
                    selected.add(option)
                }

                override fun onUnselected(option: String) {
                    unselected.add(option)
                }
            }
        ).select("b")
        assertEquals(listOf("b"), selected)
        assertEquals(listOf("a", "c"), unselected)
    }

    private class Delegates {
        var flag by emaBooleanDelegate<Delegates>(false)
        var value by emaSyncDelegate("a")
        val trigger by emaStartTriggerDelegate()
    }

    @Test
    fun `delegates store their values`() {
        val delegates = Delegates()
        assertFalse(delegates.flag)
        delegates.flag = true
        assertTrue(delegates.flag)
        assertEquals("a", delegates.value)
        delegates.value = "b"
        assertEquals("b", delegates.value)
        assertSame(delegates.trigger, delegates.trigger)
        assertFalse(delegates.trigger.hasBeenStarted)
    }

    @Test
    fun `navigation nodes keep the history`() {
        val first = EmaNavigationNode("a")
        val second = first.next("b")
        val third = second.next("c")
        assertEquals("b", third.back()?.value)
        assertTrue(third.hasPreviousNode(first))
        assertFalse(first.hasPreviousNode(third))
        assertTrue(third.hasPreviousNodeValue("a"))
        assertFalse(third.hasPreviousNodeValue("z"))
        assertNull(first.previous)
    }
}

private class Doubler : EmaUseCase<Int, Int>() {
    override suspend fun useCaseFunction(input: Int) = input * 2
}

private class Counter : EmaFlowUseCase<Int, Int>() {
    override fun useCaseFunction(input: Int): Flow<Int> = flowOf(*(1..input).toList().toTypedArray())
}

private class Upper : EmaSyncUseCase<String, String> {
    override fun invoke(input: String) = input.uppercase()
}

@OptIn(ExperimentalCoroutinesApi::class)
class EmaCoroutinesTest {

    @Test
    fun `use cases run on the given dispatcher`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        assertEquals(4, Doubler()(2, dispatcher))
        assertEquals(listOf(1, 2, 3), Counter()(3, dispatcher).toList())
        assertEquals("EMA", Upper()("ema"))
    }

    @Test
    fun `requireMinDurationOf waits at least the given time`() = runTest {
        val result = EmaUtils.Coroutines.requireMinDurationOf(500.milliseconds) { "done" }
        assertEquals("done", result)
        // The real time spent is subtracted, so the virtual delay can be a few milliseconds shorter
        assertTrue(testScheduler.currentTime >= 400)
    }

    @Test
    fun `suspendCoroutineWithTimeout resumes or times out`() = runTest {
        assertEquals(1, EmaUtils.Coroutines.suspendCoroutineWithTimeout<Int>(100.milliseconds) { it.resume(1) })
        assertFailsWith<TimeoutCancellationException> {
            EmaUtils.Coroutines.suspendCoroutineWithTimeout<Int>(100.milliseconds) { }
        }
    }

    @Test
    fun `flow extensions`() = runTest {
        val values: Flow<Any> = flowOf(1, 2, "three", 4)
        assertEquals(listOf<Any>(1, 2), values.whileIsInstanceOf(Int::class).toList())
        assertEquals("three", values.untilInstanceOf(String::class))
        assertEquals(4, flowOf(1, 4, 6).until { it > 3 })
        assertEquals(listOf(1, 2, 3), flowOf(1, 2).concat(flowOf(3)).toList())
    }

    @Test
    fun `single event flow drops old values when nobody collects`() = runTest {
        val flow = EmaUtils.Flows.singleEvent<Int>()
        assertTrue(flow.tryEmit(1))
        assertTrue(flow.tryEmit(2))
        assertEquals(0, flow.replayCache.size)
    }

    @Test
    fun `first emitted value of a flow`() = runTest {
        assertEquals(1, flowOf(1, 2).first())
    }
}
