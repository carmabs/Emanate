# Source

An `EmaSource` holds a state handled with actions, in the style of Redux. It is the engine of
[`EmaViewModel`](viewmodel.md), and it can also be used on its own.

Every action that enters the source goes through the same path, one action after another:

![How an action flows through an Emanate screen](../assets/flow.svg)

1. The [middlewares](middlewares.md) decide the path of the action: let it continue, change it, delay it or stop it.
2. The [reducers](reducers.md) return the next state. It is published as soon as they return.
3. The [side effects](side-effects.md) run the business logic asynchronously, with the new state, and react
   dispatching actions, posting events or [broadcasting](broadcasts.md) actions to other screens.

[Plugins](plugins.md) group pieces of the three kinds to reuse them in many screens.

Each piece is declared for a type of action and only receives the actions of that type, so a `sealed` hierarchy of
actions lets you handle a whole screen in one `when`, or a single action with its own class.

## Setup

The source is configured once, in `setup` (the `setup` function of an `EmaViewModel`, which returns `source { }`, or
the last parameter of `EmaSource(...)`). Nothing can be added after it. Each kind of piece has its own block, in the
order an action goes through them:

```kotlin
override fun setup() = source {
    middlewares {
        add<HomeAction.QueryWritten> { /* ... */ }
        +middlewareDelayOf(HomeAction.RefreshRequested::class, 300.milliseconds)
    }
    reducers {
        add<HomeAction> { action -> /* ... */ }
        // Declared as a property of the ViewModel
        +userReducer
    }
    sideEffects {
        add<HomeAction.RefreshRequested> { /* ... */ }
    }
    broadcasts {
        listen<ProfileBroadcast.FriendAdded>()
    }
}
```

| Block             | Adds                                                                                  |
|-------------------|---------------------------------------------------------------------------------------|
| `middlewares { }` | The [middlewares](middlewares.md), which run first.                                  |
| `reducers { }`    | The [reducers](reducers.md).                                                         |
| `sideEffects { }` | The [side effects](side-effects.md).                                                 |
| `broadcasts { }`  | The types of the actions broadcast by other sources that it receives, with `listen<Type>()`. See [Broadcasts](broadcasts.md). |
| `plugins { }`     | The [plugins](plugins.md): groups of pieces that the screen doesn't need to know.    |

Inside a block, `add<Type> { }` adds a piece for the actions of that type, and `+` adds one already created.

## Pieces are values

Every piece can be replaced. Besides the lambdas, each piece can be written as a class that implements `EmaMiddleware`, `EmaReducer` or
`SideEffect`, or created with `middlewareOf`, `middlewareDelayOf`, `reducerOf` and `sideEffectOf`. Then it can be declared as a
property, shared between screens or **received in the constructor** of the ViewModel:

```kotlin
class LiveFriends(private val getFriendsUseCase: GetFriendsUseCase) :
    SideEffect<HomeState, HomeAction, HomeEvent, HomeAction.RefreshRequested> {

    override val actionType = HomeAction.RefreshRequested::class
    override val concurrency = SideEffectConcurrency.Leading

    override suspend fun SideEffectScope<HomeState, HomeAction, HomeEvent>.invoke(action: HomeAction.RefreshRequested) {
        dispatch(HomeAction.EffectQueryResult.Started)
        getFriendsUseCase(GetFriendsUseCase.Input(state.query))
            .onSuccess { friends -> dispatch(HomeAction.EffectQueryResult.Loaded(friends)) }
            .onFailure { dispatch(HomeAction.EffectQueryResult.Failed) }
    }
}

class HomeViewModel(
    private val loadFriends: SideEffect<HomeState, HomeAction, HomeEvent, HomeAction.RefreshRequested>
) : EmaViewModel<HomeState, HomeAction, HomeEvent>(HomeState()) {

    override fun setup() = source {
        sideEffects { +loadFriends }
    }
}
```

A ViewModel that receives its pieces doesn't know which ones it gets: the real API, a mock or a fake. That is how the
same screen behaves differently in production, in a demo build and in the tests. See
[Mocking and environments](../guides/environments.md).

The middlewares run in the order they are added, and so do the reducers: each one receives the state returned by the
previous one. A block can be used more than once, for example to group the pieces of each feature: its pieces are
added after the ones of the previous blocks of the same kind.

Each piece only reaches what belongs to it: a reducer can't add other pieces nor dispatch, and a side effect can't
add other side effects. The properties of the ViewModel, like its use cases, can be used everywhere.

### Reusing pieces between screens

To share a group of pieces between screens, write an extension function of `SourceSetupScope` with its blocks and
call it inside `source { }`. As the blocks can be used more than once, its pieces are added in the place where it is
called, together with the ones of the screen:

```kotlin
fun <S : EmaState, A : EmaAction, E : EmaEvent> SourceSetupScope<S, A, E>.analytics(tracker: Tracker, screen: String) {
    middlewares {
        add<EmaAction> { action ->
            tracker.track(action::class.simpleName.orEmpty())
            next()
        }
    }
    sideEffects {
        add<EmaLifecycle.Resumed> { tracker.screenShown(screen) }
    }
}

override fun setup() = source {
    analytics(tracker, screen = "Home")
    reducers {
        add<HomeAction> { action -> /* ... */ }
    }
}
```

A generic function like this one can be used by any screen. Its side effects can only dispatch actions of type `A`,
the actions of the screen that calls it, so a function that dispatches its own actions is written for the types of
one screen: it is useful to split a big ViewModel by feature. The function only reaches what it receives as
parameters, so pass it the use cases it needs.

## Using a source on its own

A source doesn't depend on any ViewModel. Create it with `EmaSource(...)`, observe `stateFlow` and `eventFlow`, and
call `close()` when it isn't needed anymore: it processes the pending actions and cancels its work.

```kotlin
val counter = EmaSource<CounterState, CounterAction, CounterEvent>(CounterState()) {
    reducers {
        add<CounterAction.Increment> { state.copy(count = state.count + 1) }
    }
}

counter.dispatch(CounterAction.Increment)
counter.close()
```

By default it uses the [configuration](../guides/configuration.md) of Emanate and runs on its `main` dispatcher; pass
another one in the `configuration` parameter.
