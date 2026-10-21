# Actions

An action is **something the user did**: "pulled to refresh", "wrote a query", "confirmed the dialog".
Actions go from the view to the ViewModel.

```kotlin
sealed interface HomeAction : EmaAction {

    data object RefreshRequested : HomeAction
    data class QueryWritten(val query: String) : HomeAction
    data class FriendClicked(val friend: User) : HomeAction
    data object ErrorAccepted : HomeAction
}
```

## Naming

Name actions after **what the user did**, in the past tense, not after what the ViewModel should do.
`QueryWritten` is better than `UpdateQuery`: the view reports the fact and the ViewModel decides what to do with it.

Use a `sealed interface` so that `when` is exhaustive. When you add an action the compiler tells you everywhere you must handle it.

## Handling actions

Declare in the `setup` of the ViewModel the middlewares, reducers and side effects of each type of action. With the
`sealed` type, a reducer handles every action of the screen in an exhaustive `when`, and a side effect can handle a
single action:

```kotlin
override fun setup() = source {
    reducers {
        add<HomeAction> { action ->
            when (action) {
                is HomeAction.QueryWritten -> state.copy(query = action.query)
                is HomeAction.ErrorAccepted -> state.copy(dialog = null)
                // ...
            }
        }
    }
    sideEffects {
        add<HomeAction.RefreshRequested> { /* ... */ }
    }
}
```

See [Source](source.md). The public entry point of the ViewModel is `dispatch(action)`, so a view can only communicate
through actions and cannot call arbitrary functions of the ViewModel.

## Actions are also results

The side effects report what happened with actions too, so the reducers can apply it. Group them in a sealed type
inside the actions of the screen, named after the side effect that dispatches them, so it is clear that only the
ViewModel sends them:

```kotlin
sealed interface HomeAction : EmaAction {
    // ...

    // The results of the search, dispatched by its side effect
    sealed interface EffectQueryResult : HomeAction {
        data object Started : EffectQueryResult
        data class Loaded(val friends: List<User>) : EffectQueryResult
        data object Failed : EffectQueryResult
    }
}
```

As every change of the state comes from an action, the log of the actions tells the whole story of the screen, and a
test can check it step by step.

## Dispatching from the view

A Compose screen content receives the state and an `EmaImmutableActionDispatcher`, a stable wrapper that does not
cause unnecessary recompositions. Every callback of the UI only reports what the user did with `actions.dispatch`:

```kotlin
class HomeScreenContent : EmaComposableScreenContent<HomeState, HomeAction, HomeEvent> {

    @Composable
    override fun onState(state: HomeState, actions: EmaImmutableActionDispatcher<HomeAction>) {
        Column {
            // The user writes a query
            OutlinedTextField(
                value = state.query,
                onValueChange = { query -> actions.dispatch(HomeAction.QueryWritten(query)) }
            )
            // The user asks for a refresh
            Button(onClick = { actions.dispatch(HomeAction.RefreshRequested) }) {
                Text(text = "Refresh")
            }
            // The user clicks a friend
            FriendList(
                friends = state.friends,
                onFriendClick = { friend -> actions.dispatch(HomeAction.FriendClicked(friend)) }
            )
        }
        // The user accepts the error dialog
        if (state.dialog == HomeDialog.LoadError) {
            AlertDialog(
                onDismissRequest = { actions.dispatch(HomeAction.ErrorAccepted) },
                confirmButton = {
                    TextButton(onClick = { actions.dispatch(HomeAction.ErrorAccepted) }) { Text(text = "OK") }
                },
                text = { Text(text = "The friends couldn't be loaded") }
            )
        }
    }
}
```

The callbacks never change the state nor decide what happens next: the ViewModel does, in its reducers and side
effects. Child composables like `FriendList` receive plain callbacks, so they don't depend on Emanate and can be
reused or previewed on their own.

For previews use `EmaImmutableActionDispatcher.EMPTY`, which ignores every action. Outside Compose,
`EmaActionDispatcher.EMPTY` does the same.

## Special action types

| Type                      | Use                                                               |
|---------------------------|-------------------------------------------------------------------|
| `EmaAction`               | The actions of a screen. Your actions extend this.                |
| `EmaInitializer`          | The data a screen starts with. See [Initializers](../guides/initializers.md). |
| `EmaLifecycle`            | The lifecycle of the view: `Started`, `Resumed`, `Paused`, `Stopped` and `Destroyed`. See [ViewModel](viewmodel.md#the-lifecycle-as-actions). |
| `EmaRestoreState`         | The values restored by a [backup](../guides/saving-state.md) after the death of the process. |
| `EmaAction.EMPTY`         | The action type of a screen without actions.                      |

Any of them can be handled by the middlewares, reducers and side effects, like the actions of the screen.
