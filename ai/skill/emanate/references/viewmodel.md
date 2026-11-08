# ViewModel pieces

`setup() = source { middlewares{} reducers{} sideEffects{} broadcasts{} plugins{} }`. `setup` runs once, lazily, so it can use constructor properties. Pieces of a kind run in the order added; blocks can repeat.
`add<Type> { }` creates a piece; `add(piece)`, `add(a, b)`, `add(list)` install existing ones.

## Side effects

Inside: `state` (always latest), the `action`, `dispatch`, `publish`, `broadcast`. They are `suspend`.

| Option | Use |
|---|---|
| `concurrency = Every` (default) / `Latest` (cancel previous) / `Leading` (ignore while running) | Choose on purpose |
| `timeout = 10.seconds, onTimeout = { ... }` | Give up and report |
| `survivesClose = true` | Finish after the screen closes (saving), up to `closeTimeout` |
| `onClose { broadcast(...) }` | Work when the screen closes |
| `name = "x"` | Identify it in errors/listeners (default: action type) |

Never change state or call UI from here; `dispatch` an action and let a reducer apply it. Expected failures: use case returns `EmaResult`, dispatch an action per case. Unexpected exceptions follow the configured error policy.

## Middlewares (checkpoints before reducers)

Use for analytics, logs, debounce, feature flags, simulated latency. Call `next()` exactly once (forgetting it swallows the action; twice throws).
`next()` continue, `next(other)` replace, don't call = stop, `nextAfter(id = "search") { delay(300) }` debounce (same id cancels the waiting one; `stop()` inside discards).

## Lifecycle as actions

`EmaInitializer` (once), `EmaLifecycle.Started/Resumed/Paused/Stopped/Destroyed`. Handle them like any action: `add<EmaLifecycle.Resumed> { dispatch(HomeAction.RefreshRequested) }`.
`override val updateOnInitialization = false` delays the first draw until the first action/initializer (avoids empty flash).

## Pieces as values (injectable)

```kotlin
private val refresh = sideEffectOf(HomeAction.RefreshRequested::class, concurrency = SideEffectConcurrency.Leading) { ... }
private val reducer = reducerOf(HomeAction::class) { action -> ... }
override fun setup() = source { reducers { add(reducer) }; sideEffects { add(refresh) } }
```
Or implement `SideEffect<S, A, E, ActionType>` as a class (override `actionType`, `concurrency`, `suspend fun SideEffectScope<S, A, E>.invoke(action)`) and receive it in the constructor.

## Broadcasts

Declare `sealed interface XBroadcast : EmaAction`. Sender: `broadcast(XBroadcast.Created(f))` in a side effect. Receiver: `broadcasts { listen<XBroadcast.Created>() }` then handle with `add<XBroadcast.Created>` in reducers/side effects. Without `listen` nothing fails and nothing happens: `assertSetup()` in a test catches it.

## Plugins

Groups of pieces reused across screens (`plugins { add(...) }`); prefer them to ViewModel inheritance. Install `plugins {}` before `reducers {}` when a reducer depends on what a plugin restores.

## Configuration

`Emanate.init(EmaConfiguration.Android(debug = BuildConfig.DEBUG))` once in `Application.onCreate`, before any ViewModel. Debug builds log every action (state before/after) and fail fast; release keeps working. Custom logger: `.copy(logger = EmaLogger { tag, msg -> ... })`. A ViewModel can take `configuration: EmaConfiguration = Emanate.configuration` for explicit configuration (useful in tests).

## EmaSource

The engine inside the ViewModel, usable without a screen (state shared by several screens). ViewModel = EmaSource + lifecycle/initializer actions + logs.
