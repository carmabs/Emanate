# ViewModel

An `EmaViewModel` is a plain Kotlin class with no platform dependencies. It holds the state of a screen and handles
each action with the middlewares, reducers and side effects declared in its `setup`.

## EmaViewModel

Extend `EmaViewModel<State, Action, Event>`, pass the initial state and declare in `setup` how the actions are
handled. `source { }` creates the setup with the types of the ViewModel, so they aren't written again:

```kotlin
class HomeViewModel(
    private val getFriendsUseCase: GetFriendsUseCase,
    initialDataState: HomeState
) : EmaViewModel<HomeState, HomeAction, HomeEvent>(initialDataState) {

    override fun setup() = source {
        reducers {
            add<HomeAction> { action ->
                when (action) {
                    is HomeAction.QueryWritten -> state.copy(query = action.query)
                    is HomeAction.RefreshRequested -> state.copy(errorQueryTooShort = state.query.length == 1)
                    is HomeAction.EffectQueryResult.Started -> state.copy(isLoading = true)
                    is HomeAction.EffectQueryResult.Loaded -> state.copy(isLoading = false, friends = action.friends)
                    is HomeAction.EffectQueryResult.Failed -> state.copy(isLoading = false, dialog = HomeDialog.LoadError)
                    // ...
                }
            }
        }
        sideEffects {
            add<HomeAction.RefreshRequested>(concurrency = SideEffectConcurrency.Leading) {
                if (state.errorQueryTooShort) return@add
                dispatch(HomeAction.EffectQueryResult.Started)
                getFriendsUseCase(GetFriendsUseCase.Input(state.query))
                    .onSuccess { friends -> dispatch(HomeAction.EffectQueryResult.Loaded(friends)) }
                    .onFailure { dispatch(HomeAction.EffectQueryResult.Failed) }
            }
            add<HomeAction.FriendClicked> { action -> postEvent(HomeEvent.FriendSelected(action.friend)) }
        }
    }
}
```

| In `source { }`                    | Purpose                                                                    |
|------------------------------------|----------------------------------------------------------------------------|
| `middlewares { add<Type> { } }`    | Decides the path of the actions of that type before the reducers: `next()`, `nextAfter { }`... |
| `reducers { add<Type> { } }`       | Returns the next state for the actions of that type. Pure: it only reads `state`. |
| `sideEffects { add<Type> { } }`    | Business logic run after the reducers: use cases, `dispatch`, `postEvent`. |
| `broadcasts { listen<Type>() }`    | Receives the actions of that type broadcast by other ViewModels. See [Results between screens](broadcasts.md). |
| `plugins { +plugin }`              | Installs a group of pieces, like a [backup of the state](../guides/saving-state.md). |

Inside each block, `+` adds a piece already created, like `+middlewareDelayOf(...)`, a reducer declared as a property
or a side effect received in the constructor. That is what lets you
[swap the behaviour of a screen by environment](../guides/environments.md).

Everything about middlewares, reducers, side effects and plugins is in [Source](source.md).

`setup` is called the first time the ViewModel is used, not in the constructor, so it can use the properties of your
class, like its use cases.

The constructor receives, besides the initial state, the `configuration` of the ViewModel: by default the one set with
`Emanate.init`. It decides where the ViewModel runs, how its errors are handled and where its actions are logged.
See [Configuration](../guides/configuration.md).

## The lifecycle as actions

`EmaViewModel` has no lifecycle hooks: the lifecycle of the view and its initializer arrive as actions, which the
middlewares, reducers and side effects handle like any other. Only the ViewModel receives them, from the screen.

![The lifecycle of a screen as actions](../assets/lifecycle.svg)

| Action                       | When                                                                     |
|------------------------------|--------------------------------------------------------------------------|
| `EmaInitializer`             | **Once**, the first time the screen is created: its [initializer](../guides/initializers.md), or `EmaInitializer.EMPTY`. |
| `EmaLifecycle.Started`       | The screen becomes visible.                                              |
| `EmaLifecycle.Resumed`       | The screen is in the foreground.                                         |
| `EmaLifecycle.Paused`        | The screen is no longer fully visible.                                   |
| `EmaLifecycle.Stopped`       | The screen goes to the background.                                       |
| `EmaLifecycle.Destroyed`     | The last action, right before the ViewModel is closed.                   |

The initializer arrives **once**, the first time the screen is created, so recreating the screen doesn't load the data
again. As they are actions, a reducer can change the state when they arrive, and a side effect can react to them:

```kotlin
override fun setup() = source {
    reducers {
        // The initializer gives the screen its first state
        add<HomeInitializer> { initializer ->
            when (initializer) {
                is HomeInitializer.HomeUser -> state.copy(user = initializer.user)
            }
        }
        // The dialog is closed when the user leaves the screen
        add<EmaLifecycle.Stopped> { state.copy(dialog = null) }
    }
    sideEffects {
        // The friends are loaded once, when the screen is created
        add<HomeInitializer> { dispatch(HomeAction.RefreshRequested) }
        // And refreshed every time the user comes back to the screen
        add<EmaLifecycle.Resumed> { dispatch(HomeAction.RefreshRequested) }
    }
}
```

Work that only makes sense while the user sees the screen, like a timer or location updates, starts and stops with
`Started` and `Stopped`.

### When the ViewModel is closed

After `Destroyed`, the ViewModel is closed and its work is cancelled. Work that must run when the screen is closed,
like sending a result to the previous screen, goes in an `onClose` block, which can finish up to the `closeTimeout`
of the [configuration](../guides/configuration.md), 5 seconds by default:

```kotlin
sideEffects {
    onClose {
        broadcast(HomeBroadcast.LastSearch(state.query))
    }
}
```

It can read the state and broadcast actions. The actions it dispatches and the events it posts are ignored, as the
ViewModel is closed. A side effect added with `survivesClose = true`, like a request that saves what the user did, can
also finish after the ViewModel is closed.

## The contract with the view

The views don't depend on the implementation of the ViewModel, but on `EmaViewModelContract<S, E>`: the initial
state, `stateFlow`, `eventFlow`, `consumeEvent` and the lifecycle calls (`onCreated`, `onStartView`, `onResumeView`,
`onPauseView`, `onStopView` and `close`). `EmaViewModel` turns each call into the action of the table above.

## How the ViewModel is kept alive

The screen integration keeps your ViewModel as long as its screen exists, and across the recreations of the view, like
a configuration change. With `emanate-compose`:

1. The first time a screen is opened, Emanate calls the `viewModel` function you gave it and keeps that instance.
2. While the screen exists, when the view is recreated or the user comes back to it, Emanate reuses the instance it
   keeps. Your function is not called again.
3. When the screen is closed, the ViewModel receives `EmaLifecycle.Destroyed` and is closed. Opening the screen again
   creates a new one.

Each screen therefore has its own ViewModel, created once. See [Dependency injection](../guides/dependency-injection.md).

## Rendering before the first update

`updateOnInitialization` (protected, `true` by default) controls whether a Compose screen draws the initial state
straight away. Override it with `false` to render only after the first action of the screen or initializer other
than `EmaInitializer.EMPTY`, for example to avoid a flash of empty content.
