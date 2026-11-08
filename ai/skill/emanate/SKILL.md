---
name: emanate
description: Write, review or test code that uses the Emanate library (com.github.carmabs.ema, package prefix "ema"/"emanate"): EmaViewModel, EmaState, EmaAction, EmaEvent, EmaScreenContent, emaScreen, EmaSource, reducers, side effects, middlewares, broadcasts, backup/process death, emanate-test. Use when creating a screen or ViewModel, handling actions/events/dialogs/navigation/back/permissions, injecting environments (mock/dev/pro), or writing ViewModel tests with Emanate.
---

# Emanate

Unidirectional architecture for Kotlin. The view sends **actions**, the ViewModel returns **state** and one-shot **events**.
Every action follows the path: middlewares -> reducers -> side effects. Pieces are values, so they can be injected.

Docs: https://github.com/carmabs/Ema/tree/master/docs. Read the `references/` file for your task only when you need it.

## The five pieces of a screen

```kotlin
data class HomeState(val query: String = "", val isLoading: Boolean = false, val dialog: HomeDialog? = null) : EmaState

sealed interface HomeAction : EmaAction {
    data class QueryWritten(val query: String) : HomeAction      // what the USER did, past tense
    data object RefreshRequested : HomeAction
    sealed interface SideEffects : HomeAction {                  // results, only sent by the ViewModel
        data object Started : SideEffects
        data class Loaded(val friends: List<User>) : SideEffects
        data object Failed : SideEffects
    }
}

sealed interface HomeEvent : EmaEvent {                          // what HAPPENED, once. Never "GoTo..."
    data class FriendSelected(val friend: User) : HomeEvent
}

class HomeViewModel(
    private val getFriends: GetFriendsUseCase,                   // dependencies come in the constructor
    initialState: HomeState = HomeState()
) : EmaViewModel<HomeState, HomeAction, HomeEvent>(initialState) {

    override fun setup() = source {                              // runs once; pieces run in the order added
        reducers {
            add<HomeAction> { action ->                          // pure: state + action -> new state
                when (action) {                                  // exhaustive on the sealed type
                    is HomeAction.QueryWritten -> state.copy(query = action.query)
                    HomeAction.SideEffects.Started -> state.copy(isLoading = true)
                    is HomeAction.SideEffects.Loaded -> state.copy(isLoading = false)
                    HomeAction.SideEffects.Failed -> state.copy(isLoading = false, dialog = HomeDialog.LoadError)
                    HomeAction.RefreshRequested -> state
                }
            }
        }
        sideEffects {
            add<HomeAction.RefreshRequested>(concurrency = SideEffectConcurrency.Leading) {
                dispatch(HomeAction.SideEffects.Started)
                getFriends(GetFriendsUseCase.Input(state.query))                  // EmaResult
                    .onSuccess { dispatch(HomeAction.SideEffects.Loaded(it)) }
                    .onFailure { dispatch(HomeAction.SideEffects.Failed) }
            }
        }
    }
}

class HomeScreenContent : EmaScreenContent<HomeState, HomeAction> {  // stateless, no-arg constructor
    @Composable
    override fun onState(state: HomeState, actions: EmaActionDispatcher<HomeAction>) { /* draw; actions.dispatch(...) */ }

    @Preview @Composable
    private fun PreviewLoading() = onState(HomeState(isLoading = true), EmaActionDispatcher.EMPTY)
}

// In the NavHost (emanate-compose-android-nav2). Initialize once: Emanate.init(EmaConfiguration.Android(debug = BuildConfig.DEBUG))
emaScreen(
    screenContent = HomeScreenContent(),
    viewModel = { get<HomeViewModel>() },                        // creates a NEW one per screen (factory, never single)
    onEvent = { navigator.handleHomeEvent(it) }
)
```

## Rules (do / don't)

| Do | Don't |
|---|---|
| Change state only in reducers, with `copy` | Mutate state, or change it from a side effect/middleware/view |
| Reducers pure: no use cases, clock, random, `dispatch`/`publish` | Call the backend in a reducer |
| Work in side effects; one action per step (`Started`/`Loaded`/`Failed`) and report with `dispatch`, `publish`, `broadcast` | Put business logic in the ViewModel body or the view |
| Pick `concurrency` on purpose: `Leading` for taps, `Latest` for search, `Every` only for independent work | Leave the default `Every` on user actions |
| ViewModel: pure Kotlin, no `Context`/`View`/`R.string`. Texts in state as `EmaText` | `String` translated in the state, platform types in state |
| Call repositories only from use cases (`EmaUseCase`, one `Input` data class inside, return `EmaResult` if it can fail) | Repository in a ViewModel, several loose params |
| View only does `actions.dispatch(...)` and reads state | Call other ViewModel functions from the view |
| Derived values as properties of the state (`val showEmpty get() = ...`) | Logic in the composable |
| Dialog = nullable sealed field in state (`dialog: HomeDialog? = null`); buttons dispatch actions; reducer sets `null` | `show()`/`dismiss()` or local `remember` state for dialogs |
| Navigation in a **navigator** (one per graph, plain class, one `handleXEvent` per screen) reacting to events | Navigate from the ViewModel, or name events `GoToX`/`OpenX` |
| Events named for what happened: `FriendSelected`, `LoginSuccess`, `CloseClicked` | Events as commands: `ShowToast`, `GoToHome` |
| Keep `onEvent` short; long UI work in your own coroutine scope | Block `onEvent` |
| Constructor-inject use cases, state and (optionally) pieces so mock/dev/pro/test swap them | Create dependencies inside the ViewModel |
| One feature = one package in `presentation` and `ui` with the same name; naming `<Screen>State/Action/Event/ViewModel/ScreenContent/Initializer/Dialog` | Packages by type (`states/`, `viewmodels/`) |
| Use a base `BaseViewModel` of your own, even if empty | Deep ViewModel hierarchies; prefer plugins/composition |
| Previews: `Preview<What>` private functions inside the screen content, using `EmaActionDispatcher.EMPTY` | A real ViewModel in a preview |
| Screens that need no state/actions/events: `EmaState.EMPTY`, `EmaAction.EMPTY`, `EmaEvent.EMPTY` (and if all three, a plain composable) | |
| Broadcast to other screens with `broadcast(...)` **and** `broadcasts { listen<Type>() }` in the receiver | Forget `listen` (silent failure) |
| State for what must be there when the user comes back; events for one-shots | An event for something that must survive recreation |

## Module layout (recommended)

`presentation` and `domain` = pure Kotlin (use `api` for what appears in public signatures: state models, `emanate-core`).
`ui` = composables + navigators. `data` = repository implementations. `app` = Application + DI.
Artifacts: `emanate-core` (KMP), `emanate-android`, `emanate-compose-android`, `emanate-compose-android-nav2`, `emanate-serialization`, `emanate-test`.

## Where to read next (only when needed)

- Screens, events, navigator, initializers, dialogs, back, permissions, `EmaScreen` without NavHost -> `references/compose.md`
- Middlewares, plugins, broadcasts, `survivesClose`, timeouts, lifecycle actions, pieces as values -> `references/viewmodel.md`
- Saving state across process death (`backupOf`) -> `references/process-death.md`
- Injecting environments (mock/dev/pro) and Koin -> `references/di-environments.md`
- Unit tests (`emanate-test`) -> `references/testing.md`
- `EmaText`, `EmaResult`, use cases -> `references/utils.md`
