# Saving state across process death

Android can kill your app while it is in the background, for example while the user goes to the system settings to
grant a permission. When the user comes back, the screen is created again with its
[initializer](initializers.md), but what the user typed is lost.

Emanate solves it with a **backup**: a [plugin](../concepts/plugins.md) that saves the parts of the state you
choose every time they change, and restores them when the screen is created again. The ViewModel stays pure Kotlin:
the platform only gives it the store where the values are kept.

```kotlin
class ProfileCreationViewModel(
    initialDataState: ProfileCreationState,
    private val stateStore: EmaStateStore
) : EmaViewModel<ProfileCreationState, ProfileCreationAction, ProfileCreationEvent>(initialDataState) {

    override fun setup() = source {
        plugins {
            +backupOf(stateStore) {
                add(
                    key = "form",
                    codec = json(),
                    save = { ProfileCreationForm(name, surname) },
                    restore = { form -> copy(name = form.name, surname = form.surname) }
                )
            }
        }
        reducers { /* ... */ }
    }
}

@Serializable
data class ProfileCreationForm(val name: String, val surname: String)
```

That's all. There are no save callbacks, no `SavedStateHandle` and no Android code in the ViewModel.

## The store

`EmaStateStore` keeps the saved values as text. On Android, Emanate gives each screen a store over the
`SavedStateHandle` of its destination: the `viewModel` function of the screen receives it, so pass it to the ViewModel.

```kotlin
createComposableScreen(
    screenContent = ProfileCreationScreenContent(),
    viewModel = { store -> ProfileCreationViewModel(ProfileCreationState.DEFAULT, store) },
    onEvent = { /* ... */ }
)
```

With Koin, pass it as a parameter. See [Dependency injection](dependency-injection.md#the-store-of-a-backup). Where
nothing has to be saved, for example on other platforms, pass `EmaStateStore.None`.

## The values

Each value has a unique key, a function that reads it from the state and another one that puts it back:

| In `backupOf { }`                           | Saves                                                                  |
|---------------------------------------------|------------------------------------------------------------------------|
| `add(key, save, restore)`                   | A `String`.                                                            |
| `add(key, codec, save, restore)`            | A value of any type, converted to text with an `EmaBackupCodec`.       |
| `+value`                                    | A value already created.                                               |

`save` returns `null` to save nothing. The values are restored in the order they are added.

### Codecs

| Codec                     | Module                  | Saves                                                          |
|---------------------------|-------------------------|----------------------------------------------------------------|
| `EmaBackupCodec.Text`     | `emanate-core`          | Text as it is.                                                 |
| `json()`                  | `emanate-serialization` | Any `@Serializable` class or basic type, in JSON. Inside `add`, the type is taken from `save`; elsewhere write it: `json<ProfileCreationForm>()`. |
| Your own `EmaBackupCodec` | Your code               | Anything, for example an enum by its name.                     |

```kotlin
dependencies {
    implementation("com.github.carmabs.ema:emanate-serialization:$emanateVersion")
}
```

A value that can't be decoded, for example one saved by a previous version of the app with other fields, is not
restored: the screen keeps its initial state.

## When the values are restored

The backup restores the values right after the first action of type `restoreOn`: by default the
[initializer](initializers.md), which the ViewModel always receives when its screen is created. So the initializer
can't overwrite what the user typed: it is applied first, and the saved values on top of it.

![How a backup restores and saves the state](../assets/backup.svg)

The values arrive as an `EmaRestoreState` action, which goes through the middlewares and reducers like any other.
Handle it if the restored state needs something else, for example to validate the form again:

```kotlin
reducers {
    add<EmaRestoreState> { state.copy(canCreate = state.name.isNotBlank()) }
}
```

Install the backup before those reducers, so it has already restored its values when they run.

## Testing it

`emanate-test` has a store in memory. Pass it to a ViewModel and then to a new one, to simulate the death of the
process: the new one restores what the first one saved.

```kotlin
@Test
fun `the form survives the death of the process`() = runTest {
    val store = testStateStore()
    val first = ProfileCreationViewModel(ProfileCreationState.DEFAULT, store)
    first.onCreated(ProfileCreationInitializer.Admin)
    first.dispatch(ProfileCreationAction.UserNameWritten("Ana"))

    val restored = ProfileCreationViewModel(ProfileCreationState.DEFAULT, store)
    restored.onCreated(ProfileCreationInitializer.Admin)

    assertEquals("Ana", restored.stateFlow.value.name)
}
```

`store.values` shows what is saved, and `restoreStateOf("form" to "...")` creates the `EmaRestoreState` to check it
with `assertActions`.

## Tips

- **Save only what the user would lose**: form fields, selected items, the current step. The platform limits the size
  of the saved values.
- **Don't save what you can load again**, like lists from the network: load them again from the initializer.
- **One backup per source.** A backup can't be installed in two ViewModels: create one in each.
