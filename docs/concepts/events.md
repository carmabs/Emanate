# Events

An event is **something that happened in the feature** and that the view must handle **once**:
show a message, open another screen, close the screen.

```kotlin
sealed interface HomeEvent : EmaEvent {
    data class FriendsUpdated(val count: Int) : HomeEvent
    data class FriendAdded(val friend: User) : HomeEvent
    data class FriendSelected(val friend: User) : HomeEvent
}
```

## Publishing events

Events are published from the side effects of the ViewModel, with `publish`:

```kotlin
sideEffects {
    add<HomeAction.SideEffects.QueryLoaded> { action -> publish(HomeEvent.FriendsUpdated(action.friends.size)) }
    add<HomeAction.FriendClicked> { action -> publish(HomeEvent.FriendSelected(action.friend)) }
}
```

An event arrives after the actions dispatched before it, so the view sees the state they produced.

## Handling events

The view handles each event once, in the order they were published. A Compose screen handles all of them in one place: the
`onEvent` lambda of `emaScreen`, in the navigation graph. There the `NavController`, the navigator and the `Context`
are at hand, so it can navigate, show a message or do anything else:

```kotlin
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            val navigator = remember { HomeNavigator(navController) }
            NavHost(navController, startDestination = HomeScreenContent::class.routeId) {
                emaScreen(
                    screenContent = HomeScreenContent(),
                    viewModel = { get<HomeViewModel>() },
                    onEvent = { event ->
                        when (event) {
                            is HomeEvent.FriendSelected -> navigator.goToProfile(event.friend)
                            is HomeEvent.FriendsUpdated -> toast("${event.count} friends")
                            is HomeEvent.FriendAdded -> toast("${event.friend.name} is now your friend")
                        }
                    }
                )
            }
        }
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
```

As every event arrives at the same `when`, the compiler warns you when a new event is not handled. If the `when` grows,
move it to a function of the navigator. See [Compose screens](../compose/screens.md#handling-the-events).

## Naming

**Events say what happened, never what to do.** The receiver decides the reaction.

| Prefer                 | Avoid                 | Why                                                               |
|------------------------|-----------------------|-------------------------------------------------------------------|
| `FriendSelected(user)` | `OpenProfile(user)`   | The ViewModel does not know that selecting means navigating.      |
| `LoginSuccess(user)`   | `GoToHome`            | Which screen follows is the navigator's decision.                 |
| `CloseClicked`         | `CloseScreen`         | The view could also just hide a pane.                             |
| `FriendAdded(user)`    | `ShowToast(user)`     | It could be a toast, a snackbar or nothing at all.                |

A good test: the name still makes sense if you replace the screen's UI completely.

## Delivery rules

- **Once.** After `onEvent` returns, the event is consumed. It is not replayed when the view is recreated.
- **Not lost.** Events are queued in the ViewModel until a view is able to receive them. If you publish an event while the screen is in the background, or while it is being recreated, it is delivered afterwards.
- **In order.** Several pending events are delivered as a batch, in the order they were published.
- **Equal events.** While an event is waiting to be delivered, publishing an equal one does nothing, so a double tap does
  not show the same message twice. Once the view has handled it, an equal event is a new one and is delivered again.
  To queue an equal event while the first one is still waiting, pass `allowDuplicated = true`:

```kotlin
publish(HomeEvent.FriendAdded(ana), allowDuplicated = true)
```

- **Only while the screen is visible.** Compose screens receive events while the lifecycle is `STARTED`.
- **Not persisted.** Events live in the ViewModel, so they survive configuration changes but not process death.

> **Keep `onEvent` short.** The events are delivered one after another, and an event is consumed when its `onEvent`
> returns. Navigating or showing a toast is immediate. For something that takes time, like a snackbar that waits for
> the user, launch it in a coroutine scope of yours, such as the one of `rememberCoroutineScope()`, so the next events
> do not wait for it. Send long-running work back to the ViewModel as an action.

## Events or state

If it must be there when the user comes back, it is [state](state.md), not an event.
See the table in [Architecture](architecture.md#state-or-event).

## Screens with no events

A screen that never publishes events uses `EmaEvent.EMPTY` as its event type, and its `onEvent` does nothing:

```kotlin
class SettingsViewModel : EmaViewModel<SettingsState, SettingsAction, EmaEvent.EMPTY>(SettingsState()) {
    // ...
}

emaScreen(
    screenContent = SettingsScreenContent(),
    viewModel = { SettingsViewModel() },
    onEvent = {}
)
```

In the same way, `EmaState.EMPTY` is the state of a screen with no state, and `EmaAction.EMPTY` the actions of a screen
the user can't interact with. A screen with no state, actions nor events has no logic: draw it with a plain
composable, without Emanate.
