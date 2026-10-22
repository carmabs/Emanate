# Emanate

**Emanate is a unidirectional architecture for Kotlin where every behaviour of a screen is a small piece you can
swap, intercept, reuse and test on its own.**

You describe a screen with three plain types (a **state**, the **actions** of the user and the one-shot **events**) and
an `EmaViewModel` that handles each action with **middlewares**, **reducers** and **side effects**. Emanate takes care
of everything around them: lifecycle, coroutines, concurrency, navigation, process death, logs and errors.

![How an action flows through an Emanate screen](assets/flow.svg)

| Building block | What it is                                                           | You write                                          |
|----------------|----------------------------------------------------------------------|----------------------------------------------------|
| **State**      | An immutable data class with everything the screen shows.            | `data class HomeState(...) : EmaState`             |
| **Action**     | Something the user did.                                              | `sealed interface HomeAction : EmaAction`          |
| **Event**      | Something that happened in the feature, delivered to the view once.  | `sealed interface HomeEvent : EmaEvent`            |

The view **renders** the state, **reports** what the user did with actions and **reacts** to events. It never holds
business logic.

## A screen in a few lines

Each piece is declared for the type of action it handles. This home screen searches the friends of the user as they
type:

```kotlin
class HomeViewModel(private val getFriendsUseCase: GetFriendsUseCase) :
    EmaViewModel<HomeState, HomeAction, HomeEvent>(HomeState()) {

    override fun setup() = source {
        middlewares {
            // Debounce: only the last query typed in 300 ms continues
            add<HomeAction.QueryWritten> { nextAfter(id = "search") { delay(300) } }
        }
        reducers {
            // The next state for every action of the screen
            add<HomeAction> { action ->
                when (action) {
                    is HomeAction.QueryWritten -> state.copy(query = action.query)
                    is HomeAction.EffectQueryResult.Started -> state.copy(isLoading = true)
                    is HomeAction.EffectQueryResult.Loaded -> state.copy(isLoading = false, friends = action.friends)
                    is HomeAction.EffectQueryResult.Failed -> state.copy(isLoading = false, dialog = HomeDialog.LoadError)
                    is HomeAction.FriendClicked -> state
                }
            }
        }
        sideEffects {
            // Searches with the new query, cancelling the previous search if it is still running
            add<HomeAction.QueryWritten>(concurrency = SideEffectConcurrency.Latest) {
                dispatch(HomeAction.EffectQueryResult.Started)
                getFriendsUseCase(GetFriendsUseCase.Input(state.query))
                    .onSuccess { friends -> dispatch(HomeAction.EffectQueryResult.Loaded(friends)) }
                    .onFailure { dispatch(HomeAction.EffectQueryResult.Failed) }
            }
            // Tells the view to open the profile
            add<HomeAction.FriendClicked> { action -> postEvent(HomeEvent.FriendSelected(action.friend)) }
        }
    }
}
```

- The **middleware** waits until the user stops typing before the query continues.
- The **reducer** returns the next state for each action, so the view always draws a valid state.
- The **side effects** run the business logic and report it with actions (`HomeAction.EffectQueryResult`) or events.

The same pieces can also be declared as properties of the ViewModel, with `middlewareOf`, `reducerOf` and
`sideEffectOf`, and installed with `+`. Then each one can be reused, received in the constructor to
[change it by environment](guides/dependency-injection.md), or tested on its own:

```kotlin
class HomeViewModel(private val getFriendsUseCase: GetFriendsUseCase) :
    EmaViewModel<HomeState, HomeAction, HomeEvent>(HomeState()) {

    private val searchDebounce = middlewareOf(HomeAction.QueryWritten::class) {
        nextAfter(id = "search") { delay(300) }
    }

    private val homeReducer = reducerOf(HomeAction::class) { action ->
        when (action) {
            is HomeAction.QueryWritten -> state.copy(query = action.query)
            is HomeAction.EffectQueryResult.Started -> state.copy(isLoading = true)
            is HomeAction.EffectQueryResult.Loaded -> state.copy(isLoading = false, friends = action.friends)
            is HomeAction.EffectQueryResult.Failed -> state.copy(isLoading = false, dialog = HomeDialog.LoadError)
            is HomeAction.FriendClicked -> state
        }
    }

    private val searchFriends = sideEffectOf(HomeAction.QueryWritten::class, SideEffectConcurrency.Latest) {
        dispatch(HomeAction.EffectQueryResult.Started)
        getFriendsUseCase(GetFriendsUseCase.Input(state.query))
            .onSuccess { friends -> dispatch(HomeAction.EffectQueryResult.Loaded(friends)) }
            .onFailure { dispatch(HomeAction.EffectQueryResult.Failed) }
    }

    private val openFriend = sideEffectOf(HomeAction.FriendClicked::class) { action ->
        postEvent(HomeEvent.FriendSelected(action.friend))
    }

    override fun setup() = source {
        middlewares { +searchDebounce }
        reducers { +homeReducer }
        sideEffects {
            +searchFriends
            +openFriend
        }
    }
}
```

Start with [Getting started](getting-started.md), or see what these pieces make possible in
[Why Emanate](why-emanate.md).
