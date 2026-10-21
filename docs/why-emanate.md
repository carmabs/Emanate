# Why Emanate

Most architectures tell you where to put your code. Emanate also changes **what your code is made of**.

In a classic ViewModel the behaviour of a screen is code inside its functions: to change it you edit the ViewModel, add
an `if` or mock a whole repository. In Emanate every behaviour is a piece of code that reacts to an action, and every
piece can be replaced:

| Piece           | Runs                                   | Its job                                                         |
|-----------------|----------------------------------------|-----------------------------------------------------------------|
| **Middleware**  | First, when an action arrives          | Decide the path of the action: let it continue, change it, delay it or stop it. |
| **Reducer**     | Then, synchronously                    | Return the next state. A pure function.                         |
| **Side effect** | After the reducers, asynchronously     | Run the business logic and report the result with actions or events. |
| **Broadcast**   | Sent from a side effect                | Send an action to the other screens that listen to its type, like a result for the previous screen. |
| **Plugin**      | Installed in the setup                 | Group a set of pieces (middlewares, reducers, side effects) that work together, so they can be reused in different screens, like analytics or the backup of the state. |

A piece can be a lambda written in the screen, a class, or an object received in the constructor. So the behaviour of
a screen can be composed, replaced and inspected like any other data, and that unlocks things a classic architecture
can't do without rewriting the screen.

## 1. Change how a screen behaves just by changing what you inject

The ViewModel receives the side effect that loads the friends of the user. Production injects the real one, the demo
build a mock and the tests a fake. The ViewModel is the same in the three of them.

![The same ViewModel in three environments](assets/environments.svg)

```kotlin
class HomeViewModel(private val loadFriends: FriendsEffect) :
    EmaViewModel<HomeState, HomeAction, HomeEvent>(HomeState()) {

    override fun setup() = source {
        reducers { /* ... */ }
        sideEffects {
            +loadFriends
        }
    }
}

// Production
HomeViewModel(LiveFriends(getFriendsUseCase))

// Demo build: no backend, a fixed list after half a second
HomeViewModel(MockFriends(friends = demoFriends))
```

**Without Emanate** the usual options are an `if (isDemo)` inside the ViewModel, a mocking library in the tests, or a
fake repository for the whole app. Here you replace exactly the behaviour you want, and nothing else.
See [Mocking and environments](guides/environments.md).

## 2. Middlewares: step in before any action runs

A middleware sees every action of its type before the business logic and decides what happens to it, without the
ViewModel knowing it. Inject them only where you need them:

```kotlin
// Two seconds of latency before every refresh, in the QA build
middlewareDelayOf<HomeState, HomeAction, HomeAction.RefreshRequested>(HomeAction.RefreshRequested::class, 2.seconds)

// Every refresh fails, to check the error dialog against the real backend
middlewareOf<HomeState, HomeAction, HomeAction.RefreshRequested>(HomeAction.RefreshRequested::class) {
    next(HomeAction.EffectQueryResult.Failed)
}
```

The same mechanism debounces a search field, stops the actions of a feature behind a flag or an expired session,
replaces an action with another one, or tracks every action for analytics. Testing loading states and error dialogs
no longer needs a broken network or a special backend. See [Middlewares](concepts/middlewares.md).

## 3. Plugins: write cross-cutting behaviour once and install it anywhere

A plugin is a reusable setup: it groups a set of middlewares, reducers and side effects that work together, and any
screen installs it in one line. Analytics, logs, session checks, feature flags or the backup of the state are needed
by many screens, so they are written once, as a plugin.

This one tracks every action of the screen and when the screen is shown:

```kotlin
class ScreenAnalytics<S : EmaState>(private val tracker: Tracker, private val screen: String) :
    EmaSourcePlugin<S, Nothing> {

    override fun install(setup: SourceSetupScope<S, Nothing, Nothing>) {
        // Sees every action of the screen before the reducers
        setup.middlewares {
            add<EmaAction> { action ->
                tracker.track(screen, action::class.simpleName.orEmpty())
                next()
            }
        }
        // Reacts to the lifecycle of the screen
        setup.sideEffects {
            add<EmaLifecycle.Resumed> { tracker.screenShown(screen) }
        }
    }
}
```

And any screen installs it:

```kotlin
override fun setup() = source {
    plugins {
        +ScreenAnalytics<HomeState>(tracker, screen = "Home")
    }
    reducers { /* ... */ }
}
```

No inheritance, no annotations and no code generation. See [Plugins](concepts/plugins.md).

## 4. Concurrency under control

Each side effect has built-in mechanisms to manage the work queued for its actions: ignore new actions while one is
running, cancel the running work when a new action arrives, or run them all in parallel, with timeouts and debounce.
The classic bugs of a screen (the button tapped twice, the search that shows old results, the request triggered on
every keystroke) are one parameter away:

```kotlin
sideEffects {
    // A second refresh while one is running is ignored
    add<HomeAction.RefreshRequested>(concurrency = SideEffectConcurrency.Leading) { /* ... */ }
    // A new query cancels the previous search
    add<HomeAction.QueryWritten>(concurrency = SideEffectConcurrency.Latest) { /* ... */ }
}
middlewares {
    // Debounce: only the last query of the last 300 ms continues
    add<HomeAction.QueryWritten> { nextAfter(id = "search") { delay(300) } }
}
```

See [Async work and errors](guides/async-and-errors.md#concurrency-timeouts-and-debounce).

## 5. Results between screens, as typed actions

A screen can send a result to other screens by broadcasting an action. Every ViewModel that listens to its type
receives it as one more action of its own, and handles it with its own reducers and side effects. No shared
ViewModels, no result APIs of the platform and no casts:

```kotlin
// The profile screen broadcasts the friend it created
sideEffects {
    add<ProfileAction.SaveClicked> { broadcast(ProfileBroadcast.FriendCreated(state.friend), keepUntilReceived = true) }
}

// The home screen adds it to its list
override fun setup() = source {
    reducers {
        add<ProfileBroadcast.FriendCreated> { action -> state.copy(friends = state.friends + action.friend) }
    }
    broadcasts {
        listen<ProfileBroadcast.FriendCreated>()
    }
}
```

With `keepUntilReceived`, the result waits for the first screen that listens to it, even if that screen is not created
yet. See [Results between screens](concepts/broadcasts.md).

## 6. The whole history of a screen, with no extra code

Every action, state change, event and side effect goes through the same pipeline, so everything that happens on the
screen is traced without writing a line for it. The testing module records that history in order, so a test checks
the whole flow step by step, with virtual time:

```kotlin
val viewModel = HomeViewModel(MockFriends(friends = listOf(ana)))
val recorder = viewModel.record()

viewModel.dispatch(HomeAction.RefreshRequested)
viewModel.dispatch(HomeAction.FriendClicked(ana))
advanceUntilIdle()

recorder.assertState { it.friends == listOf(ana) }
recorder.assertEvents(HomeEvent.FriendSelected(ana))
```

`assertSetup()` even fails when a piece can never run, like the reducer of a result that the screen forgot to listen
to. See [Testing](guides/testing.md).

## 7. Survive process death declaratively

The system can kill your app while the user is in another one. A backup plugin saves the fields you choose and
restores them when the screen comes back, with no platform code in the ViewModel:

```kotlin
plugins {
    +backupOf(stateStore) {
        add("search", save = { query }, restore = { saved -> copy(query = saved) })
    }
}
```

See [Saving state](guides/saving-state.md).

## 8. One policy for every error

Every `EmaViewModel` logs each action it receives and the state before and after it. The errors of any middleware,
reducer or side effect go through a single configurable [error policy](guides/async-and-errors.md#errors): fail at
once while developing, report and keep working in production. A `sideEffectListener` measures every piece of business
logic.

## 9. Multiplatform

`emanate-core` is pure Kotlin Multiplatform, with no platform dependency. The same ViewModels run in your app, in your
unit tests and on every platform Kotlin targets, so the presentation layer can be shared between mobile, desktop and
the web. See [Kotlin Multiplatform](guides/multiplatform.md).

## The rules that make it work

Emanate stays predictable because it keeps a few rules: the state only changes in the reducers, the view only renders
and reports, and anything that lasts belongs in the state while anything that happens once is an event. Read them in
[Architecture](concepts/architecture.md).
