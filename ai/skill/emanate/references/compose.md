# Compose: screens, events, navigation, dialogs, back, permissions

Artifacts: `emanate-compose-android` (screens, no navigation dependency) + `emanate-compose-android-nav2` (`emaScreen`, `emaDialog`).

## Registering

```kotlin
NavHost(navController, startDestination = HomeScreenContent::class.routeId) {
    emaScreen(
        screenContent = HomeScreenContent(),
        viewModel = { store -> get<HomeViewModel> { parametersOf(store) } }, // lambda; store only if it has a backup
        initializerSupport = EmaInitializerSupport.fromRoute(HomeInitializer.serializer()), // only if it receives data
        transitionAnimation = EmaAnimTransition(enterTransition = {...}, exitTransition = {...}), // optional
        onEvent = { navigator.handleHomeEvent(it) }
    )
}
```

- Route: `ScreenContent::class.routeId` (unique per class). `navController.navigate(HomeScreenContent::class.routeId)`.
- `emaDialog(screenContent, viewModel, dialogProperties, onEvent)`: a destination shown as a dialog. For a small dialog inside a screen use state.
- `viewModel` lambda is called once per screen opening; the ViewModel lives while the destination exists.
- `EmaScreen(vm = { ... }, screenContent, onEvent, initializer, previewRenderState)`: same without a NavHost (single-screen activity, or a screen inside a bigger layout). `previewRenderState` is drawn only in IDE previews.

## Events and navigator

All events of a screen reach `onEvent` once, in order, only while the lifecycle is STARTED. Not persisted across process death.
`when` over a sealed event is exhaustive. Create **one navigator per graph** with `remember`, one function per screen:

```kotlin
class MainNavigator(private val navController: NavController) {
    fun handleLoginEvent(event: LoginEvent, onMessage: (LoginEvent) -> Unit) = when (event) {
        is LoginEvent.LoginSuccess -> navController.navigate(
            route = HomeScreenContent::class.routeId,
            initializerBundle = EmaInitializerBundle(
                HomeInitializer.HomeUser(event.user),
                BundleSerializerStrategy.kSerialization(HomeInitializer.serializer())
            )
        )
        is LoginEvent.Message -> onMessage(event)           // not navigation: whoever shows messages handles it
    }
    // HomeEvent.CloseClicked -> navController.navigateBack()
}
```

Equal events: while one waits to be delivered an equal one is dropped; pass `publish(e, allowDuplicated = true)` to queue it.
Slow UI (snackbar): `scope.launch { snackbarHostState.showSnackbar(...) }` from `onEvent`, never block it.

## Initializers (data for a screen)

```kotlin
@Serializable
sealed interface HomeInitializer : EmaInitializer {
    @Serializable data class HomeUser(val user: User) : HomeInitializer
}
```
Arrives **once** as an action (`EmaInitializer.EMPTY` if none). Handle in a reducer (apply to state) and/or a side effect (load data). Not re-sent on rotation; re-sent after process death, and a backup restores typed values on top.

## Dialogs (driven by state)

```kotlin
sealed interface NoteDialog { data object ConfirmDelete : NoteDialog }
data class NoteState(val dialog: NoteDialog? = null) : EmaState
// reducers: DeleteClicked -> state.copy(dialog = ConfirmDelete); DeleteCancelled/Confirmed -> state.copy(dialog = null)
// view:  if (state.dialog == NoteDialog.ConfirmDelete) AlertDialog(onDismissRequest = { actions.dispatch(DeleteCancelled) }, ...)
```
Buttons, outside-tap and back all dispatch actions. Close dialogs on `EmaLifecycle.Stopped` if they must not survive leaving the screen.

## Back handling

```kotlin
override fun onBack(state: NoteState): NoteAction? = if (state.dialog == null) NoteAction.OnBack else null
```
Return an action to intercept system back (the ViewModel decides: open a "discard?" dialog, publish a close event), `null` for default. The toolbar arrow dispatches the same action. Never create a `BackHandler` yourself.

## Permissions (Android)

1. Declare in the manifest. 2. ViewModel sets `requestingPermission = true` in state. 3. The screen content asks with `rememberEmaPermissionManager()` (`requestPermission`, `requestMultiplePermission`, `isPermissionGranted`; `suspend`, one request at a time) in a `LaunchedEffect` keyed on the flag, and dispatches `PermissionAnswered(PermissionState)`. Results: `GRANTED`, `NOT_GRANTED`, `NOT_GRANTED_SHOULD_EXPLAIN` (explain with a dialog, then retry). After a second refusal only the app settings can grant it. In previews the manager grants everything.

## Previews

`@Preview` functions inside the screen content, named `Preview<What>`, one per interesting state (empty, loading, error dialog). The IDE instantiates the content, so keep its constructor parameterless.
