package com.carmabs.emanate.core.viewmodel

import com.carmabs.emanate.core.Emanate
import com.carmabs.emanate.core.action.EmaAction
import com.carmabs.emanate.core.action.EmaEventConsumer
import com.carmabs.emanate.core.action.EventDispatcher
import com.carmabs.emanate.core.error.EmaErrorPolicy
import com.carmabs.emanate.core.error.runHandlingErrors
import com.carmabs.emanate.core.initializer.EmaInitializer
import com.carmabs.emanate.core.model.EmaConfiguration
import com.carmabs.emanate.core.model.reflection.Reflection
import com.carmabs.emanate.core.sideeffect.SideEffectResult
import com.carmabs.emanate.core.state.EmaEvent
import com.carmabs.emanate.core.state.EmaState
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass
import kotlin.reflect.cast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * View model to handle view states.
 *
 * @param initialDataState state of the view before any update
 * @param configuration applied by the view model. By default the one of [Emanate], read when the view model is
 * created; pass one to configure this view model without depending on [Emanate.init], for example in a library or in
 * a test. The view model runs on its main dispatcher: it creates its own scope with it and cancels it when it is
 * cleared.
 *
 * @author <a href="mailto:apps.carmabs@gmail.com">Carlos Mateo Benito</a>
 */
abstract class EmaViewModelClassic<S : EmaState, E : EmaEvent>(
    initialDataState: S,
    private val configuration: EmaConfiguration = Emanate.configuration
) : EmaViewModelContract<S, E> {

    private val eventDispatcher = EventDispatcher<E>()
    private val singleSideEffectMap by lazy {
        hashMapOf<String, Job>()
    }

    /**
     * The scope where coroutines are launched by default. It is cancelled when the view model is cleared.
     */
    protected val scope: CoroutineScope = CoroutineScope(SupervisorJob() + configuration.dispatchers.main)

    /**
     * To determine if the view must be updated when view model is created automatically
     */
    protected open val updateOnInitialization: Boolean = true

    /**
     * Used to know if state has been updated at least once
     */
    private var hasBeenUpdated = false

    /**
     * Used to know if subscribed view should render the state
     */
    final override val shouldRenderState: Boolean
        get() {
            return updateOnInitialization || hasBeenUpdated
        }

    /**
     * Observable state that launch event every time a value is set. This value will be the state
     * of the view.
     */
    private val mStateFlow: MutableStateFlow<S> = MutableStateFlow(initialDataState)

    final override val eventFlow: Flow<List<E>> = eventDispatcher.eventFlow

    final override fun consumeEvent(event: E) = eventDispatcher.consumeEvent(event)

    private var firstTimeResumed = true

    /**
     * Determine if the viewmodel has initialized its state
     */

    protected var hasBeenInitialized: Boolean = false
        private set

    override fun onCreated(initializer: EmaInitializer?) {
        if (!hasBeenInitialized) {
            hasBeenInitialized = true
            if (updateOnInitialization) {
                mStateFlow.tryEmit(state)
            }
            handlingErrors("onStateCreated") { onStateCreated(initializer) }
            handlingErrors("onBroadcastListenerSetup") { onBroadcastListenerSetup() }
        }
    }

    final override fun onStartView() {
        handlingErrors("onViewStarted") { onViewStarted() }
    }

    /**
     * Called when view is shown in foreground
     */
    final override fun onResumeView() {
        handlingErrors("onViewResumed") { onViewResumed() }
        firstTimeResumed = false
    }

    /**
     * Called when view is hidden in background
     */
    final override fun onPauseView() {
        handlingErrors("onViewPaused") { onViewPaused() }
    }

    final override fun onStopView() {
        handlingErrors("onViewStopped") { onViewStopped() }
    }

    /**
     * Called when the state of the view has been created
     */
    abstract fun onStateCreated(initializer: EmaInitializer? = null)

    /**
     * Called always the view goes to the foreground
     */
    protected open fun onViewResumed() = Unit

    /**
     * Called always the view is not fully visible
     */
    protected open fun onViewPaused() = Unit

    /**
     * Called when the view is visible to the user
     */
    protected open fun onViewStarted() = Unit

    /**
     * Called always the view goes to the background
     */
    protected open fun onViewStopped() = Unit

    final override val stateFlow: StateFlow<S> = mStateFlow.asStateFlow()

    /**
     * When a background task must be executed for data retrieving or other background job, it must
     * be called through this method with [action] function
     * @param action is the function that will be executed in background
     * @param dispatcher where the useCase is launched by default
     * @param logName name reported in [Reflection], in the errors and the side effect listener of the
     * configuration. If it is null, "sideEffect" is used
     * @return The EmaUseCaseResult where you can handle the result with the methods
     * - onSuccess when the result of action function is successful
     * - onError when the action function has thrown an error
     * - onFinish when the action function has ended, independently if an error has been thrown
     * - job returns the job where the action function has been executed
     */
    protected fun <T> sideEffect(
        dispatcher: CoroutineContext = this.scope.coroutineContext,
        throwException: Boolean = shouldThrowException(),
        logName: String? = null,
        action: suspend CoroutineScope.() -> T
    ): SideEffectResult<T> = launchSideEffect(dispatcher, throwException, logName, action)

    private fun <T> launchSideEffect(
        dispatcher: CoroutineContext,
        throwException: Boolean,
        logName: String?,
        action: suspend CoroutineScope.() -> T
    ) = SideEffectResult(
        container = this,
        scope = scope,
        dispatcher = dispatcher,
        logName = logName,
        configuration = configuration,
        throwException = throwException,
        action = action
    )

    protected fun <T> singleSideEffect(
        id: String,
        dispatcher: CoroutineContext = this.scope.coroutineContext,
        throwException: Boolean = shouldThrowException(),
        logName: String? = null,
        action: suspend CoroutineScope.() -> T
    ): SideEffectResult<T> {
        singleSideEffectMap[id]?.cancel()
        val handler = launchSideEffect(dispatcher, throwException, logName, action)
        singleSideEffectMap[id] = handler.job
        return handler
    }

    private fun shouldThrowException() = configuration.errorPolicy is EmaErrorPolicy.Throw

    /**
     * Called when the view model is closed
     */
    protected open fun onDestroy() = Unit

    /**
     * Normal state content of the view
     */
    final override val initialState: S = initialDataState

    /**
     * The state of the view.
     */
    protected var state: S = initialState
        private set

    /**
     * Called once, when the view model is created. Register here the listeners of the broadcast actions with
     * [registerBroadcastListener].
     */
    protected open fun onBroadcastListenerSetup() = Unit

    /**
     * Update the current state and update the normal view state by default
     * @param changeStateFunction create the new state
     */
    protected fun updateState(changeStateFunction: S.() -> S) {
        state = state.changeStateFunction()
        hasBeenUpdated = true
        mStateFlow.value = state
    }

    /**
     * Dispatches a one-shot event to be consumed by the view
     * @param event The event to be dispatched.
     * @param allowDuplicated If true, allows the same event to be dispatched multiple times before being consumed.
     */
    protected fun postEvent(event: E, allowDuplicated: Boolean = false) {
        eventDispatcher.postEvent(event, allowDuplicated)
    }

    /**
     * Broadcasts any action to the view models and sources listening to its type, added with
     * [registerBroadcastListener] or in the broadcasts block of setup. For example, to send a result to the previous
     * screen. It is delivered to the ones listening at the moment.
     * @param keepUntilReceived if nobody is listening to it, it is kept and delivered to the first one that listens to
     * its type. For example, when the previous screen is created again after the process has been killed.
     */
    protected fun broadcast(action: EmaAction, keepUntilReceived: Boolean = false) {
        configuration.broadcaster.broadcast(action, keepUntilReceived)
    }

    /**
     * Listens to the actions of type [A] broadcast by other view models or sources while this view model is alive.
     * Call it in [onBroadcastListenerSetup].
     */
    protected inline fun <reified A : EmaAction> registerBroadcastListener(noinline receiver: (A) -> Unit) {
        registerBroadcastListener(A::class, receiver)
    }

    /**
     * Implementation of the inline registerBroadcastListener with the type of the actions. It starts undispatched, so
     * it is already listening when it returns.
     */
    @PublishedApi
    internal fun <A : EmaAction> registerBroadcastListener(actionType: KClass<A>, receiver: (A) -> Unit) {
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            configuration.broadcaster.listen(listOf(actionType)).collect { action ->
                // With Catch the listener keeps receiving after an error
                handlingErrors("broadcast listener of ${actionType.simpleName}") { receiver(actionType.cast(action)) }
            }
        }
    }

    /**
     * Closes the view model when its screen is destroyed. It cancels all background pending tasks.
     */
    final override fun close() {
        scope.cancel()
        handlingErrors("onDestroy") { onDestroy() }
    }

    /**
     * Runs [block], code of the view model called by Emanate, applying the error policy of the configuration. With
     * Catch the error is reported as thrown in [where], and the changes made in the state before it are kept.
     */
    internal fun handlingErrors(where: String, block: () -> Unit) {
        configuration.runHandlingErrors(this, { where }, block)
    }
}
