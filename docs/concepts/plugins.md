# Plugins

A plugin is a **reusable setup**: it groups a set of middlewares, reducers, side effects and broadcast listeners that
work together, and any screen installs all of them in one line.

![A plugin groups pieces and is installed in many screens](../assets/plugin.svg)

Use a plugin for a feature that many screens need, but that has nothing to do with what each screen does: analytics,
a session check, a feature flag, the [backup of the state](../guides/process-death.md)... Without plugins, that code is
copied in every ViewModel, or hidden in a base class that every screen must extend.

## Installing a plugin

Plugins are installed in the `plugins` block of the `setup`, with `+`:

```kotlin
override fun setup() = source {
    plugins {
        +ScreenAnalytics<HomeState>(tracker, screen = "Home")
    }
    reducers { /* ... */ }
}
```

Its pieces are added to the setup of the screen, where the `plugins` block is written, and work together with the
pieces of the screen.

## Writing a plugin

A plugin implements `EmaSourcePlugin` and adds its pieces in `install`, with the same blocks as `setup`. This one
tracks every action of the screen and when the screen is shown:

```kotlin
class ScreenAnalytics<S : EmaState>(private val tracker: Tracker, private val screen: String) :
    EmaSourcePlugin<S, Nothing> {

    override fun install(setup: SourceSetupScope<S, Nothing, Nothing>) {
        // A middleware sees every action of the screen before the reducers
        setup.middlewares {
            add<EmaAction> { action ->
                tracker.track(screen, action::class.simpleName.orEmpty())
                next()
            }
        }
        // A side effect reacts to the lifecycle of the screen
        setup.sideEffects {
            add<EmaLifecycle.Resumed> { tracker.screenShown(screen) }
        }
    }
}
```

- **What it needs, it receives in the constructor**, like the `tracker`. It can't be installed without it.
- **`S` is the state** of the screens where it can be installed. A generic `S`, as here, works in any screen; write it
  when installing it: `ScreenAnalytics<HomeState>(...)`.
- **The second type is the type of the actions the plugin dispatches.** `Nothing` means it dispatches none.

## A plugin with its own actions

A plugin can't post events, because the events belong to each screen. To tell the screen something, it dispatches
actions, and the screen decides what to do with them. Which actions it can dispatch is the second type of the plugin.
Usually they are actions that the plugin defines, like the one below, so it works in any screen. A plugin written for
one specific screen could also use the actions of that screen as its second type, but then it only works there.

This plugin stops the actions of the user when the session has expired, and tells the screen with
`SessionAction.Expired`:

```kotlin
sealed interface SessionAction : EmaAction {
    data object Expired : SessionAction
}

class SessionGuard<S : EmaState>(private val session: Session) : EmaSourcePlugin<S, SessionAction> {

    override fun install(setup: SourceSetupScope<S, SessionAction, Nothing>) {
        setup.middlewares(MiddlewarePosition.First) {
            add<EmaAction> { action ->
                // The lifecycle and the actions of the plugin always continue
                if (action is EmaLifecycle || action is SessionAction || session.isActive) {
                    next()
                } else {
                    dispatch(SessionAction.Expired)
                }
            }
        }
    }
}
```

The screen installs it and reacts to its action like to any other:

```kotlin
override fun setup() = source {
    plugins {
        +SessionGuard<HomeState>(session)
    }
    sideEffects {
        add<SessionAction.Expired> { postEvent(HomeEvent.SessionExpired) }
    }
}
```

## Where its pieces go

The pieces of a plugin run in an order that depends on their kind.

**Reducers, side effects and broadcast listeners** stay **where the `plugins` block is written**, in order with the
pieces of the screen. Here, the plugin has a reducer and a side effect:

```kotlin
override fun setup() = source {
    reducers {
        add<HomeAction> { /* reducer 1 */ }
    }
    plugins {
        +MyPlugin()  // Its reducer and its side effect go here
    }
    reducers {
        add<HomeAction> { /* reducer 3 */ }
    }
}
```

When an action arrives, the reducers run in this order: reducer 1, the one of the plugin and reducer 3. Each one
receives the state returned by the previous one. The side effects are launched in the same order.

**Middlewares** can choose a position, wherever the plugin is installed: `MiddlewarePosition.First` to see every
action before anything else, like the `SessionGuard`, or `Last` to see the new state of every action, like the backup.
See [Where a middleware runs](middlewares.md#where-a-middleware-runs). Here, the plugins are written after the
middleware of the screen:

```kotlin
override fun setup() = source {
    middlewares {
        add<HomeAction> { /* middleware of the screen */ }
    }
    plugins {
        +SessionGuard<HomeState>(session)  // Its middleware is First
        +backup                            // Its middleware is Last
    }
}
```

When an action arrives, the middlewares run in this order: the one of `SessionGuard` (`First`), the one of the screen
and the one of the backup (`Last`). Then come the reducers. The position wins over the place where the plugin is
written.

## An alternative: extension functions

A plugin can't post events, because it doesn't know the events of the screen. For that, write an **extension function
of the setup** and call it inside `source { }`: it has the state, the actions and the events of the screen.

They are also useful when a ViewModel gets big. Each function groups a **subfeature** of the screen, and the ViewModel
only calls them. This one counts the loads that fail in a row, and after three of them the view suggests checking the
connection:

```kotlin
fun SourceSetupScope<HomeState, HomeAction, HomeEvent>.suggestCheckingConnection() {
    reducers {
        // Counts the failures in a row, and starts again when a load works
        add<HomeAction.EffectQueryResult> { action ->
            when (action) {
                is HomeAction.EffectQueryResult.Failed -> state.copy(failures = state.failures + 1)
                is HomeAction.EffectQueryResult.Loaded -> state.copy(failures = 0)
                is HomeAction.EffectQueryResult.Started -> state
            }
        }
    }
    sideEffects {
        // The reducer has already counted this failure
        add<HomeAction.EffectQueryResult.Failed> {
            if (state.failures == 3) postEvent(HomeEvent.CheckConnection)
        }
    }
}

class HomeViewModel : EmaViewModel<HomeState, HomeAction, HomeEvent>(HomeState()) {

    override fun setup() = source {
        suggestCheckingConnection()
        reducers { /* ... */ }
        sideEffects { /* ... */ }
    }
}
```

## The plugins of Emanate

- **The backup of the state**, created with `backupOf`, saves parts of the state and restores them after the system
  kills the app. See [Surviving process death](../guides/process-death.md).
