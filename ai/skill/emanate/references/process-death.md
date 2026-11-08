# Surviving process death (backup)

Needs `emanate-serialization` for `json()`. The ViewModel stays pure Kotlin; the platform provides an `EmaStateStore`.

```kotlin
class ProfileViewModel(
    private val addFriend: AddFriendUseCase,
    initialState: ProfileState,
    private val stateStore: EmaStateStore                       // from the emaScreen viewModel lambda
) : EmaViewModel<ProfileState, ProfileAction, ProfileEvent>(initialState) {

    override fun setup() = source {
        plugins {                                                // BEFORE reducers
            add(backupOf(stateStore) {
                add(key = "search", save = { query }, restore = { copy(query = it) })      // String, no codec
                add(key = "form", codec = json(),                                          // any @Serializable
                    save = { ProfileForm(name, surname) },                                 // null = save nothing
                    restore = { copy(name = it.name, surname = it.surname) })
            })
        }
        reducers { add<EmaRestoreState> { state.copy(canCreate = state.name.isNotBlank()) } } // optional
    }
}
@Serializable data class ProfileForm(val name: String, val surname: String)

emaScreen(screenContent = ..., viewModel = { store -> ProfileViewModel(addFriend, ProfileState.DEFAULT, store) }, onEvent = ...)
```

- Keys must be unique. Restored in the order added. A value that fails to decode is skipped (initial state kept).
- Restores right after the first `restoreOn` action (default: the initializer, so typed values win over initializer data). Another type: `backupOf(stateStore, restoreOn = ScreenOpened::class)`; until it arrives nothing is restored or saved.
- `EmaRestoreState` is an action; handle it only if the restored state needs follow-up.
- No store (no process kill, or tests that ignore it): `EmaStateStore.None`. In tests: `testStateStore()` / `restoreStateOf(...)`.
- Save only what the user typed or chose, not data that can be reloaded.
