---
name: emanate
description: Write, review or test Kotlin code that uses the Emanate library (imports from com.carmabs.emanate), including EmaViewModel, EmaState, EmaAction, EmaEvent, EmaScreenContent, emaScreen, EmaUseCase, EmaResult, EmaSource and emanate-test. Use when creating a screen, ViewModel or use case, handling actions, events, dialogs, navigation, back or permissions, deciding how to split an app into modules, switching behaviour for mocks, flavors or tests, or when code comments and uncomments lines to change behaviour.
---

# Emanate

Unidirectional architecture for Kotlin. The view sends **actions**, the ViewModel returns **state** and one-shot **events**.
Every action follows the path: middlewares -> reducers -> side effects. Pieces are values, so they can be injected.

Docs: https://github.com/carmabs/Emanate/tree/master/docs. Read the `references/` file for your task only when you need it.

## The pieces of a screen

```kotlin
data class HomeState(                                            // no default values in the constructor
    val query: String,
    val friends: List<User>,
    val isLoading: Boolean,
    val dialog: HomeDialog?
) : EmaState {
    companion object {
        val DEFAULT = HomeState(query = "", friends = emptyList(), isLoading = false, dialog = null)
    }
}

sealed interface HomeAction : EmaAction {
    data class QueryWritten(val query: String) : HomeAction      // what the USER did, past tense
    data object RefreshRequested : HomeAction
    sealed interface SideEffects : HomeAction {                  // results, only sent by the ViewModel; name the data
        data object FriendsLoadStarted : SideEffects
        data class FriendsLoaded(val friends: List<User>) : SideEffects
        data object FriendsLoadFailed : SideEffects
    }
}

sealed interface HomeEvent : EmaEvent {                          // what HAPPENED, once. Never "GoTo..."
    data class FriendSelected(val friend: User) : HomeEvent
}

class HomeViewModel(
    private val getFriends: GetFriendsUseCase,                   // constructor: use cases and state
    initialState: HomeState = HomeState.DEFAULT
) : EmaViewModel<HomeState, HomeAction, HomeEvent>(initialState) {

    override fun setup() = source {                              // runs once; pieces run in the order added
        reducers {
            add<HomeAction> { action ->                          // pure: state + action -> new state
                when (action) {                                  // exhaustive on the sealed type
                    is HomeAction.QueryWritten -> state.copy(query = action.query)
                    HomeAction.SideEffects.FriendsLoadStarted -> state.copy(isLoading = true)
                    is HomeAction.SideEffects.FriendsLoaded -> state.copy(isLoading = false, friends = action.friends)
                    HomeAction.SideEffects.FriendsLoadFailed -> state.copy(isLoading = false, dialog = HomeDialog.LoadError)
                    HomeAction.RefreshRequested -> state
                }
            }
        }
        sideEffects {
            add<HomeAction.RefreshRequested>(concurrency = SideEffectConcurrency.Leading) {
                dispatch(HomeAction.SideEffects.FriendsLoadStarted)
                getFriends(GetFriendsUseCase.Input(state.query))                  // EmaResult
                    .onSuccess { dispatch(HomeAction.SideEffects.FriendsLoaded(it)) }
                    .onFailure { dispatch(HomeAction.SideEffects.FriendsLoadFailed) }
            }
        }
    }
}

class HomeScreenContent : EmaScreenContent<HomeState, HomeAction> {  // stateless, no-arg constructor
    @Composable
    override fun onState(state: HomeState, actions: EmaActionDispatcher<HomeAction>) { /* draw; actions.dispatch(...) */ }

    @PreviewLightDark                                            // every screen content has one
    @Composable
    private fun PreviewLoading() {
        AppTheme { onState(HomeState.DEFAULT.copy(isLoading = true), EmaActionDispatcher.EMPTY) }
    }
}

// In the NavHost (emanate-compose-android-nav2). Initialize once: Emanate.init(EmaConfiguration.Android(debug = BuildConfig.DEBUG))
emaScreen(
    screenContent = HomeScreenContent(),
    viewModel = { HomeViewModel(getFriends) },                   // a NEW instance per screen, from your DI or by hand
    onEvent = { navigator.handleHome(it) }
)
```

## Rules (do / don't)

| Do | Don't |
|---|---|
| State: constructor without default values + `companion object { val DEFAULT }` with every field filled; previews and tests start from `DEFAULT.copy(...)` | Default values in the state constructor |
| Change state only in reducers, with `copy` | Mutate state, or change it from a side effect/middleware/view |
| Reducers pure: no use cases, clock, random, `dispatch`/`publish` | Call the backend in a reducer |
| Work in side effects; one action per step, named after the data (`FriendsLoadStarted`/`FriendsLoaded`/`FriendsLoadFailed`), and report with `dispatch`, `publish`, `broadcast` | Put business logic in the ViewModel body or the view |
| Pick `concurrency` on purpose: `Leading` for taps, `Latest` for search, `Every` only for independent work | Leave the default `Every` on user actions |
| ViewModel: pure Kotlin, no `Context`/`View`/`R.string`. Texts in state as `EmaText` | `String` translated in the state, platform types in state |
| Logical operations in use cases, even when they combine several repositories/managers. One `Input` data class inside. Repositories only from use cases | Use cases as CRUD pass-throughs with the logic elsewhere; repository in a ViewModel |
| Use case by type: `EmaSyncUseCase` (sync), `EmaUseCase` (suspend), `EmaFlowUseCase` (Flow) | `suspend` for something synchronous |
| Controlled errors as modelled types (`sealed interface LoginError`), in `EmaResult` when the use case can fail | Exceptions for controlled errors |
| View only does `actions.dispatch(...)` and reads state | Call other ViewModel functions from the view |
| Derived values as properties of the state (`val showEmpty get() = ...`) | Logic in the composable |
| `onBack` only decides if back is intercepted: an action, or `null` to let the OS handle it. What happens next is decided by the ViewModel | Business logic in `onBack` |
| Dialog = nullable sealed field in state (`dialog: HomeDialog?`); buttons dispatch actions; reducer sets `null` | `show()`/`dismiss()` or local `remember` state for dialogs |
| Navigation in a **navigator**: one per graph, one `handleX(event)` per screen (`handleHome`, `handleLogin`). Lambdas in the handler if something must go back to the container | Navigate from the ViewModel, or name events `GoToX`/`OpenX` |
| Events named for what happened: `FriendSelected`, `LoginSuccess`, `CloseClicked` | Events as commands: `ShowToast`, `GoToHome` |
| ViewModel constructor: use cases and state (plus `EmaStateStore` if it has a backup). Pieces only when behaviour must change per build | Create dependencies inside the ViewModel; inject pieces by default |
| Switch behaviour with pieces in the constructor or implementations per flavor (`references/di-environments.md`). If code is being commented/uncommented for this, propose it | Comment/uncomment code to mock or test |
| Modules by layer (`references/architecture.md`); package per feature; naming `<Screen>State/Action/Event/ViewModel/ScreenContent/Initializer/Dialog` | Framework code in `domain`; packages by type (`states/`, `viewmodels/`) |
| Use a base `BaseViewModel` of your own, even if empty | Deep ViewModel hierarchies; prefer plugins/composition |
| Every screen content has a preview: private `Preview<What>` inside it, `@PreviewLightDark`, app theme, `EmaActionDispatcher.EMPTY` | A real ViewModel in a preview |
| Screens with no state/actions/events: `EmaState.EMPTY`, `EmaAction.EMPTY`, `EmaEvent.EMPTY` (if all three, a plain composable) | |
| Broadcast to other screens with `broadcast(...)` **and** `broadcasts { listen<Type>() }` in the receiver | Forget `listen` (silent failure) |
| State for what must be there when the user comes back; events for one-shots | An event for something that must survive recreation |

Artifacts: `emanate-core` (KMP), `emanate-android`, `emanate-compose-android`, `emanate-compose-android-nav2`, `emanate-serialization`, `emanate-test`. Always with `implementation`, in each module that uses them.
Gradle coordinates on JitPack: `com.github.carmabs.emanate:<artifact>:<version>`.

## Where to read next (only when needed)

- Layers, modules and their dependencies, initializing libraries -> `references/architecture.md`
- Screens, events, navigator, initializers, dialogs, back, permissions, previews, `EmaScreen` without NavHost -> `references/compose.md`
- Middlewares, plugins, broadcasts, `survivesClose`, timeouts, lifecycle actions, pieces as values -> `references/viewmodel.md`
- Saving state across process death (`backupOf`) -> `references/process-death.md`
- DI, and changing behaviour for mocks, flavors, tests or demos -> `references/di-environments.md`
- Unit tests (`emanate-test`) -> `references/testing.md`
- Use cases, modelled errors, `EmaResult`, `EmaText` -> `references/utils.md`
