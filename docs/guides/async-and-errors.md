# Async work and errors

In an `EmaViewModel`, suspending work runs in the **side effects** declared in `setup`. They are launched by the actions
of their type, after their reducers, and they report the result by dispatching actions or posting events:

```kotlin
sideEffects {
    add<HomeAction.RefreshRequested>(concurrency = SideEffectConcurrency.Leading, timeout = 30.seconds) {
        dispatch(HomeAction.EffectQueryResult.Started)
        getFriendsUseCase(GetFriendsUseCase.Input(state.query))
            .onSuccess { friends -> dispatch(HomeAction.EffectQueryResult.Loaded(friends)) }
            .onFailure { dispatch(HomeAction.EffectQueryResult.Failed) }
    }
}
```

- The side effect runs on the main dispatcher of the ViewModel, and the [use cases](use-cases.md) switch to the
  background dispatcher themselves.
- It is cancelled when the ViewModel is closed, unless it is added with `survivesClose = true`.
- Cancellation is not an error: a side effect cancelled by its concurrency, its timeout or the closing of the
  ViewModel is not reported.

The whole API of the side effects is in [Source](../concepts/source.md#side-effects).

## Concurrency, timeouts and debounce

| Need                                             | How                                                                  |
|--------------------------------------------------|----------------------------------------------------------------------|
| Ignore a second tap while the work is running    | `add<A>(concurrency = SideEffectConcurrency.Leading)`                |
| Cancel the previous work when a new action comes | `add<A>(concurrency = SideEffectConcurrency.Latest)`                 |
| Run every action in parallel                     | `SideEffectConcurrency.Every`, the default                           |
| Give up after some time                          | `add<A>(timeout = 10.seconds, onTimeout = { dispatch(TimedOut) })`   |
| Debounce a field                                 | A middleware with `nextAfter(id = "search") { delay(300) }`          |

```kotlin
middlewares {
    add<HomeAction.QueryWritten> { action ->
        nextAfter(id = "search") {
            delay(300)
            if (action.query.isBlank()) stop()
        }
    }
}
sideEffects {
    add<HomeAction.QueryWritten>(concurrency = SideEffectConcurrency.Latest) { action ->
        dispatch(HomeAction.RefreshRequested)
    }
}
```

## Errors

For the errors you expect, **return a result instead of throwing**: an [`EmaResult`](use-cases.md#emaresult) makes
the failure part of the contract, and the side effect turns it into an action that the reducers show.

For the unexpected ones, the `errorPolicy` of the [configuration](configuration.md) decides what happens when a
middleware, a reducer or a side effect throws an exception:

| `EmaErrorPolicy`  | Effect |
|-------------------|--------|
| `Catch(onError)`  | **Default, and in release builds with `EmaConfiguration.Android(debug)`.** The exception is caught and written to the `logger`, and the ViewModel keeps working. `onError` also receives each one. |
| `Throw`           | The exception is thrown again and the app fails at once. **In debug builds with `EmaConfiguration.Android(debug)`.** |

With `Catch`, the action of a middleware or a reducer that fails is discarded, without changing the state, and the next
actions are processed as usual. A bug in one action doesn't break the screen.

```kotlin
Emanate.init(
    EmaConfiguration.Android(debug = BuildConfig.DEBUG).copy(
        errorPolicy = EmaErrorPolicy.Catch { error -> crashReporter.record(error.exception) }
    )
)
```

`onError` receives an `EmaError` with the exception and its `reflection`, which tells the ViewModel where it happened.

## Measuring the side effects

To track every side effect, for example to measure how long the business logic takes, set a `sideEffectListener`. Its
`onSuccess` and `onFinish` are called when each one ends:

```kotlin
EmaConfiguration.Android(debug = BuildConfig.DEBUG).copy(
    sideEffectListener = object : EmaSideEffectListener {
        override fun onFinish(reflection: Reflection) = analytics.track(reflection.methodName)
    }
)
```
