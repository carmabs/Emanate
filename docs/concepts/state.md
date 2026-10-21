# State

The state is an immutable `data class` with **everything the screen needs to draw itself**.

```kotlin
data class HomeState(
    val user: User? = null,
    val query: String = STRING_EMPTY,
    val friends: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val errorQueryTooShort: Boolean = false,
    val dialog: HomeDialog? = null
) : EmaState
```

## Rules

- **Make it a `data class`.** `copy` is how the next state is created, and its `equals` lets the view skip
  states that did not change. Use `EmaState.EMPTY` for screens with no state.
- **Make it immutable.** Use `val` and read-only collections. Change it with `copy`.
- **Give it default values** (or a `DEFAULT` instance). They become the initial state and are handy for previews and
  tests.
- **Add derived properties instead of duplicating data.** They keep the view free of logic:

```kotlin
data class HomeState(
    val user: User? = null,
    val friends: List<User> = emptyList(),
    // ...
) : EmaState {

    val showCreateButton get() = user?.role == Role.ADMIN
    val showEmptyList get() = user != null && friends.isEmpty()
}
```

## Changing the state

The state only changes in the [reducers](reducers.md) of the ViewModel, which return the next state with `copy`:

```kotlin
reducers {
    add<HomeAction> { action ->
        when (action) {
            is HomeAction.QueryWritten -> state.copy(query = action.query, errorQueryTooShort = false)
            // ...
        }
    }
}
```

The middlewares and side effects can read the current value with `state`, but never change it:

```kotlin
sideEffects {
    add<HomeAction.RefreshRequested> {
        if (state.isLoading) return@add
        // ...
    }
}
```

The state is exposed as `stateFlow: StateFlow<S>`. Because it is a `StateFlow`, equal consecutive states are not
delivered twice and slow collectors only see the latest value.

## Modelling dialogs, loading and errors

Emanate does not have special "loading" or "error" states. They are ordinary fields of your state, so they survive
a configuration change and are easy to test.

**A dialog**: a nullable sealed type. `null` means no dialog.

```kotlin
sealed interface HomeDialog {
    data object LoadError : HomeDialog
}

// Reducers
is HomeAction.EffectQueryResult.Failed -> state.copy(isLoading = false, dialog = HomeDialog.LoadError)  // show
is HomeAction.ErrorAccepted -> state.copy(dialog = null)                                    // hide
```

**Loading**: a boolean, set by the action that starts the work and cleared by the one that reports its result.

```kotlin
is HomeAction.EffectQueryResult.Started -> state.copy(isLoading = true)
is HomeAction.EffectQueryResult.Loaded -> state.copy(isLoading = false, friends = action.friends)
```

**Field errors**: one boolean (or a message) per field, named with the prefix `error`. Set it when the input is not
valid, and clear it when the user edits the field:

```kotlin
// Reducers
is HomeAction.RefreshRequested -> state.copy(errorQueryTooShort = state.query.length == 1)  // show
is HomeAction.QueryWritten -> state.copy(query = action.query, errorQueryTooShort = false)  // hide
```

The view only draws what the field says:

```kotlin
@Composable
override fun onState(state: HomeState, actions: EmaImmutableActionDispatcher<HomeAction>) {
    OutlinedTextField(
        value = state.query,
        onValueChange = { query -> actions.dispatch(HomeAction.QueryWritten(query)) },
        isError = state.errorQueryTooShort,
        supportingText = {
            if (state.errorQueryTooShort) Text(text = "Write at least two letters")
        }
    )
}
```

## Rendering the state

The view simply draws the state it receives. With Compose, recomposition does the diffing. See
[Compose screens](../compose/screens.md).

## What does *not* belong in the state

- One-shot things: messages, navigation. Use [events](events.md).
- Platform types, like a context, a view or an image of the platform. Use `EmaText` for texts with resources, see
  [Use cases and results](../guides/use-cases.md#ematext-texts-in-the-state).
- Mutable collections or objects that change without you calling `copy`.
