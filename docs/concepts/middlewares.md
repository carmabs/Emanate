# Middlewares

A middleware is a **checkpoint**: it sees an action before the [reducers](reducers.md) and the
[side effects](side-effects.md), and decides what happens to it.

![A middleware decides the path of an action](../assets/middleware.svg)

| In the middleware   | The action                                                                        |
|---------------------|-----------------------------------------------------------------------------------|
| `next()`            | Continues as it is, to the next middleware and then to the reducers.              |
| `next(other)`       | Is replaced: `other` continues instead.                                           |
| `nextAfter { }`     | Continues later, when the block finishes. The screen keeps working meanwhile.     |
| Doesn't call `next` | Stops here. The reducers and the side effects never see it.                       |

Most screens don't need middlewares. They shine for things that apply to many actions and that the business logic
shouldn't know: analytics, debounce, feature flags, simulated latency in a test build...

## Your first middleware

Middlewares are declared in the `middlewares` block of the `setup`, for a type of action. This one logs every action of
the screen and lets it continue:

```kotlin
middlewares {
    add<HomeAction> { action ->
        println("The user did $action")
        next()
    }
}
```

> **Call `next` once.** If you forget it, the action stops there, and the screen seems to ignore it.

## Stopping an action

Not calling `next` stops the action. For example, to ignore taps on a feature that is disabled:

```kotlin
middlewares {
    add<HomeAction.FriendClicked> {
        if (featureFlags.profilesEnabled) next()
    }
}
```

## Replacing an action

`next(other)` lets another action continue instead of the received one:

```kotlin
middlewares {
    // A blank query is the same as clearing the search
    add<HomeAction.QueryWritten> { action ->
        if (action.query.isBlank()) next(HomeAction.SearchCleared) else next()
    }
}
```

## Waiting: delays and debounce

`nextAfter` lets the action continue when its block finishes, without blocking the screen. Inside the block, `stop()`
discards the action.

With an `id`, a new `nextAfter` with the same id cancels the one that is still waiting. That is a **debounce**: while
the user keeps typing, only the last query continues.

```kotlin
middlewares {
    add<HomeAction.QueryWritten> { action ->
        nextAfter(id = "search") {
            delay(300)
            if (action.query.length < 2) stop()
        }
    }
}
```

For a fixed delay, `middlewareDelayOf` creates the middleware for you:

```kotlin
middlewares {
    // Every refresh waits two seconds, to see the loading state in a test build
    +middlewareDelayOf(HomeAction.RefreshRequested::class, 2.seconds)
}
```

`nextAfter` also accepts a `timeout`: if the block doesn't finish in time, the action doesn't continue and `onTimeout`
is called.

## Where a middleware runs

By default the middlewares run in the order they are written. A block can place its middlewares before or after the
rest, wherever it is written:

| `MiddlewarePosition` | Runs                                       | Use it for                                                        |
|----------------------|--------------------------------------------|-------------------------------------------------------------------|
| `First`              | Before the rest                            | Deciding the path of every action first: a session check, a feature flag. |
| `InPlace` (default)  | Where the block is written                 | The middlewares of the screen.                                    |
| `Last`               | After the rest, right before the reducers  | Observing the new state: when `next()` returns, the reducers have run. |

```kotlin
middlewares(MiddlewarePosition.First) {
    add<HomeAction> { if (session.isActive) next() }
}
```

## The rules

- **Call `next` once**, now or later with `nextAfter`.
- **Don't post events.** To tell the view something, dispatch an action with `dispatch(action)` and let a side effect
  post the event.
- **Keep the business logic in the side effects.** A middleware decides the path of the action; the work it starts
  belongs to a side effect.

## Middlewares as values

A middleware can also be created with `middlewareOf` or `middlewareDelayOf`, to declare it as a property or receive it
in the constructor. That is how a QA build adds latency to a screen without touching it:

```kotlin
class HomeViewModel(
    private val tuning: List<EmaMiddleware<HomeState, HomeAction, *>> = emptyList()
) : EmaViewModel<HomeState, HomeAction, HomeEvent>(HomeState()) {

    override fun setup() = source {
        middlewares(MiddlewarePosition.First) { tuning.forEach { +it } }
        // ...
    }
}
```

See [Mocking and environments](../guides/environments.md).
