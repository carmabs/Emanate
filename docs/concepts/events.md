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

## Posting events

Events are posted from the side effects of the ViewModel, with `postEvent`:

```kotlin
sideEffects {
    add<HomeAction.EffectQueryResult.Loaded> { action -> postEvent(HomeEvent.FriendsUpdated(action.friends.size)) }
    add<HomeAction.FriendClicked> { action -> postEvent(HomeEvent.FriendSelected(action.friend)) }
}
```

An event arrives after the actions dispatched before it, so the view sees the state they produced.

## Handling events

The view handles each event once, in the order they were posted. A Compose screen handles them in two places, and
each event reaches both: first the navigation graph, then the screen content.

**1. The navigation graph**, for the events that navigate. The `onEvent` lambda of `createComposableScreen` is where the
`NavController` and the navigator are at hand:

```kotlin
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            val navigator = remember { HomeNavigator(navController) }
            NavHost(navController, startDestination = HomeScreenContent::class.routeId) {
                createComposableScreen(
                    screenContent = HomeScreenContent(),
                    viewModel = { get<HomeViewModel>() },
                    onEvent = { event ->
                        if (event is HomeEvent.FriendSelected) navigator.goToProfile(event.friend)
                    }
                )
            }
        }
    }
}
```

**2. The screen content**, for the rest: messages, sounds, animations... Its `onEvent` receives a `context` and the
action dispatcher, next to the `onState` that draws the screen:

```kotlin
class HomeScreenContent : EmaComposableScreenContent<HomeState, HomeAction, HomeEvent> {

    @Composable
    override fun onState(state: HomeState, actions: EmaImmutableActionDispatcher<HomeAction>) {
        // Draws the screen
    }

    override suspend fun onEvent(context: Context, event: HomeEvent, actions: EmaImmutableActionDispatcher<HomeAction>) {
        when (event) {
            is HomeEvent.FriendsUpdated ->
                Toast.makeText(context, "${event.count} friends", Toast.LENGTH_SHORT).show()
            is HomeEvent.FriendAdded ->
                Toast.makeText(context, "${event.friend.name} is now your friend", Toast.LENGTH_SHORT).show()
            // Already handled by the navigation graph
            is HomeEvent.FriendSelected -> Unit
        }
    }
}
```

See [Compose screens](../compose/screens.md#two-places-to-handle-events).

## Naming

**Events say what happened, never what to do.** The receiver decides the reaction.

| Prefer                 | Avoid                 | Why                                                               |
|------------------------|-----------------------|-------------------------------------------------------------------|
| `FriendSelected(user)` | `OpenProfile(user)`   | The ViewModel does not know that selecting means navigating.      |
| `UserTypeSelected(role)`| `GoToProfileCreation`| Which screen follows is the navigator's decision.                 |
| `OnBoardingCancelled`  | `CloseScreen`         | The view could also just hide a pane.                             |
| `FriendAdded(user)`    | `ShowToast(user)`     | It could be a toast, a snackbar or nothing at all.                |

A good test: the name still makes sense if you replace the screen's UI completely.

## Delivery rules

- **Once.** After `onEvent` returns, the event is consumed. It is not replayed when the view is recreated.
- **Not lost.** Events are queued in the ViewModel until a view is able to receive them. If you post an event while the screen is in the background, or while it is being recreated, it is delivered afterwards.
- **In order.** Several pending events are delivered as a batch, in the order they were posted.
- **Equal events.** While an event is waiting to be delivered, posting an equal one does nothing, so a double tap does
  not show the same message twice. Once the view has handled it, an equal event is a new one and is delivered again.
  To queue an equal event while the first one is still waiting, pass `allowDuplicated = true`:

```kotlin
postEvent(HomeEvent.FriendAdded(ana), allowDuplicated = true)
```

- **Only while the screen is visible.** Compose screens receive events while the lifecycle is `STARTED`.
- **Not persisted.** Events live in the ViewModel, so they survive configuration changes but not process death.

> **Keep `onEvent` short.** The events are delivered one after another, and an event is consumed when its `onEvent`
> returns. While an `onEvent` is suspended, the next events wait for it. And if the screen stops being visible before
> it returns, the event is not consumed, so it is delivered again when the screen is visible. Do quick work in
> `onEvent`, and send long-running work back to the ViewModel as an action.

## Events or state

If it must be there when the user comes back, it is [state](state.md), not an event.
See the table in [Architecture](architecture.md#state-or-event).

## Screens with no events

A screen that never posts events uses `EmaEvent.EMPTY` as its event type, and doesn't implement `onEvent`:

```kotlin
class SettingsViewModel : EmaViewModel<SettingsState, SettingsAction, EmaEvent.EMPTY>(SettingsState()) {
    // ...
}

class SettingsScreenContent : EmaComposableScreenContent<SettingsState, SettingsAction, EmaEvent.EMPTY> {
    // Only onState
}
```

In the same way, `EmaState.EMPTY` is the state of a screen with no state, and `EmaAction.EMPTY` the actions of a screen
the user can't interact with. A screen with no state, actions nor events has no logic: draw it with a plain
composable, without Emanate.
