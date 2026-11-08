# Compose: screens, events, navigation, dialogs, back, permissions

Artifacts: `emanate-compose-android` (screens, no navigation dependency) + `emanate-compose-android-nav2` (`emaScreen`, `emaDialog`).

## Registering

```kotlin
NavHost(navController, startDestination = HomeScreenContent::class.routeId) {
    emaScreen(
        screenContent = HomeScreenContent(),
        viewModel = { store -> HomeViewModel(getFriends, HomeState.DEFAULT, store) },       // new instance; store only if it has a backup
        initializerSupport = EmaInitializerSupport.fromRoute(HomeInitializer.serializer()), // only if it receives data
        transitionAnimation = EmaAnimTransition(enterTransition = {...}, exitTransition = {...}), // optional
        onEvent = { navigator.handleHome(it) }
    )
}
```

- Route: `ScreenContent::class.routeId` (unique per class). `navController.navigate(HomeScreenContent::class.routeId)`.
- `emaDialog(screenContent, viewModel, dialogProperties, onEvent)`: a destination shown as a dialog. For a small dialog inside a screen use state.
- `viewModel` lambda is called once per screen opening; the ViewModel lives while the destination exists.
- `EmaScreen(vm = { ... }, screenContent, onEvent, initializer, previewRenderState)`: same without a NavHost (single-screen activity, or a screen inside a bigger layout). `previewRenderState` is drawn only in IDE previews.

## Events and navigator

All events of a screen reach `onEvent` once, in order, only while the lifecycle is STARTED. Not persisted across process death. Keep `onEvent` short.

**One navigator per graph**, a plain class created with `remember`. It has **one `handleX` per screen of the graph**, receiving that screen's sealed event in an exhaustive `when`:

```kotlin
class MainNavigator(private val navController: NavController) {

    fun handleHome(event: HomeEvent) {
        when (event) {
            is HomeEvent.FriendSelected -> navController.navigate(ProfileScreenContent::class.routeId)
            HomeEvent.CloseClicked -> navController.navigateBack()
        }
    }
}
```

If something must go back to the containing screen, add **lambdas** to the handler. The handler decides, per event, what to return through them:

```kotlin
fun handleProfile(event: ProfileEvent, onSaved: (User) -> Unit) {
    when (event) {
        is ProfileEvent.Saved -> onSaved(event.user)
        ProfileEvent.CloseClicked -> navController.navigateBack()
    }
}
```

Equal events: while one waits to be delivered an equal one is dropped; pass `publish(e, allowDuplicated = true)` to queue it.

## Initializers (data for a screen)

```kotlin
@Serializable
sealed interface HomeInitializer : EmaInitializer {
    @Serializable data class HomeUser(val user: User) : HomeInitializer
}

navController.navigate(
    route = HomeScreenContent::class.routeId,
    initializerBundle = EmaInitializerBundle(
        HomeInitializer.HomeUser(user),
        BundleSerializerStrategy.kSerialization(HomeInitializer.serializer())
    )
)
```
Arrives **once** as an action (`EmaInitializer.EMPTY` if none). Handle in a reducer (apply to state) and/or a side effect (load data). Not re-sent on rotation; re-sent after process death, and a backup restores typed values on top.

## Dialogs (driven by state)

```kotlin
sealed interface NoteDialog { data object ConfirmDelete : NoteDialog }
// state: val dialog: NoteDialog?
// reducers: DeleteClicked -> state.copy(dialog = NoteDialog.ConfirmDelete); DeleteCancelled/DeleteConfirmed -> state.copy(dialog = null)
// view:  if (state.dialog == NoteDialog.ConfirmDelete) AlertDialog(onDismissRequest = { actions.dispatch(NoteAction.DeleteCancelled) }, ...)
```
Buttons, outside-tap and back all dispatch actions.

## Back handling

`onBack` decides **when back is intercepted**: return an action to intercept it, or `null` so the OS handles it. It can read the state for that. What happens after the action is decided by the ViewModel. Don't override it if the default system back is enough.

```kotlin
override fun onBack(state: NoteState): NoteAction? =
    if (state.hasChanges && state.dialog == null) NoteAction.BackPressed else null   // null: the OS handles back

// ViewModel
reducers { add<NoteAction.BackPressed> { state.copy(dialog = NoteDialog.Discard) } }
```

While `onBack` returns an action the screen doesn't close by itself: to leave, the ViewModel publishes an event and the navigator goes back. The toolbar arrow dispatches the same action. Never create a `BackHandler` yourself.

## Permissions (Android)

1. Declare in the manifest. 2. ViewModel sets `requestingPermission = true` in state. 3. The screen content asks with `rememberEmaPermissionManager()` (`requestPermission`, `requestMultiplePermission`, `isPermissionGranted`; `suspend`, one request at a time) in a `LaunchedEffect` keyed on the flag, and dispatches `PermissionAnswered(PermissionState)`. Results: `GRANTED`, `NOT_GRANTED`, `NOT_GRANTED_SHOULD_EXPLAIN` (explain with a dialog, then retry). After a second refusal only the app settings can grant it. In previews the manager grants everything.

## Previews

**Every screen content has its preview**, annotated with `@PreviewLightDark` so it shows in light and dark mode. Write them inside the screen content, named `Preview<What>`, wrapped in the app theme (without it dark mode shows nothing different), and starting from `XState.DEFAULT`. Add one per interesting state (empty, loading, error dialog).

```kotlin
@PreviewLightDark
@Composable
private fun PreviewLoading() {
    AppTheme {
        onState(state = HomeState.DEFAULT.copy(isLoading = true), actions = EmaActionDispatcher.EMPTY)
    }
}
```

The IDE instantiates the content, so keep its constructor parameterless.
