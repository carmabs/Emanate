# Emanate (unidirectional architecture, Kotlin)

Library `com.github.carmabs.ema`. Docs: https://github.com/carmabs/Ema/tree/master/docs

Flow: the view dispatches **actions** -> middlewares -> **reducers** (new state) -> **side effects** (work). The view receives **state** and one-shot **events**.

## Pieces
- `data class XState(...) : EmaState`: immutable, default values, derived values as properties, dialog = nullable sealed field (`dialog: XDialog? = null`).
- `sealed interface XAction : EmaAction`: named for what the user DID (`QueryWritten`, not `UpdateQuery`). Results of side effects go in `XAction.SideEffects` (`Started`/`Loaded`/`Failed`).
- `sealed interface XEvent : EmaEvent`: named for what HAPPENED (`FriendSelected`, not `GoToProfile`). Use `EmaEvent.EMPTY`/`EmaState.EMPTY`/`EmaAction.EMPTY` when a screen has none.
- `class XViewModel(useCases..., initialState) : EmaViewModel<S, A, E>(initialState)` with `override fun setup() = source { reducers { add<XAction> { action -> when (action) { ... } } }; sideEffects { add<XAction.Y>(concurrency = SideEffectConcurrency.Leading) { ... } } }`.
- `class XScreenContent : EmaScreenContent<S, A>` (stateless, no-arg constructor): `@Composable override fun onState(state, actions)` draws and calls `actions.dispatch(...)`. `override fun onBack(state): A?` intercepts back. Previews: private `Preview<What>` inside it, with `EmaActionDispatcher.EMPTY`.
- Register: `emaScreen(screenContent, viewModel = { get<XViewModel>() }, onEvent = { navigator.handleXEvent(it) })` (`emanate-compose-android-nav2`), or `EmaScreen(vm=..., ...)` without a NavHost. Init once: `Emanate.init(EmaConfiguration.Android(debug = BuildConfig.DEBUG))`.

## Rules
- State changes only in reducers, via `copy`. Reducers are pure: no use cases, clock, random, `dispatch`, `publish`.
- Work lives in side effects; they report with `dispatch` (state), `publish` (event), `broadcast` (other screens, receiver needs `broadcasts { listen<Type>() }`). Never mutate state from them.
- Choose `concurrency` on purpose: `Leading` for button taps, `Latest` for search, `Every` only for independent work.
- Business logic in use cases (`EmaUseCase`, one `Input` data class inside it); return `EmaResult<T, E>` when the caller must handle failure. Repositories only from use cases.
- ViewModel is pure Kotlin: no `Context`/`View`/`R`. Texts in state as `EmaText`, resolved in the view with `stringResource()`.
- The ViewModel never navigates: it publishes events, one navigator per graph handles them. The view only dispatches actions.
- Dependencies (use cases, initial state, even pieces) come through the constructor; the `viewModel` lambda must create a new instance (Koin `factory`, not `single`).
- Dialogs, loading and field errors are state fields (`dialog`, `isLoading`, `errorX`); buttons dispatch actions.
- Initializer (`EmaInitializer`, `@Serializable`) = data for a screen, arrives once as an action.
- Process death: `plugins { add(backupOf(stateStore) { add(key, save, restore) }) }` before `reducers`; `json()` codec from `emanate-serialization`.
- Layout: package per feature, same name in `presentation` and `ui`; names `<Screen>State/Action/Event/ViewModel/ScreenContent/Initializer/Dialog`; own `BaseViewModel`; `presentation` + `domain` pure Kotlin.
- Tests (`emanate-test`): `useTestConfiguration()`/`resetTestConfiguration()`, `runTest`, `val recorder = viewModel.record()` before dispatching, `advanceUntilIdle()`, `assertStates/assertState/assertEvents/assertActions`, and `viewModel.assertSetup()`.
