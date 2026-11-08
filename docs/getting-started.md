# Getting started

## Requirements

- Android `minSdk` 24 or higher.
- The library is built with Java 21 and Kotlin 2.

## Install

Add the JitPack repository:

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

Add the dependencies:

```kotlin
val emanateVersion = "7.0.0"

dependencies {
    implementation("com.github.carmabs.emanate:emanate-android:$emanateVersion")
    implementation("com.github.carmabs.emanate:emanate-compose-android:$emanateVersion")
    // Only if your screens are in a Navigation Compose graph
    implementation("com.github.carmabs.emanate:emanate-compose-android-nav2:$emanateVersion")
}
```

For a pure Kotlin module, like a `presentation` module with the ViewModels, add only the core, and `emanate-test` to
test them:

```kotlin
dependencies {
    implementation("com.github.carmabs.emanate:emanate-core:$emanateVersion")
    testImplementation("com.github.carmabs.emanate:emanate-test:$emanateVersion")
}
```

`emanate-core` is a Kotlin Multiplatform library, so it can also be used from the `commonMain` of a multiplatform module.

## Modules

| Artifact                       | Use it for                                                                                                                                        |
| ------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------- |
| `emanate-core`                 | State, actions, events, ViewModels, `EmaSource`, plugins and use cases. Pure Kotlin Multiplatform.                                                |
| `emanate-compose-android`      | Jetpack Compose screens, back handling, dialogs, permissions and previews. It doesn't depend on any navigation library.                           |
| `emanate-compose-android-nav2` | Emanate screens in a Navigation Compose graph (`emaScreen`, `emaDialog`). See [Compose navigation](compose/navigation.md).                        |
| `emanate-android`              | The integration with the Android platform: configuration, initializers, permissions and the store of the saved state.                             |
| `emanate-serialization`        | Saves any serializable value of the state across process death. Based on kotlinx.serialization. See the [backup plugin](guides/process-death.md). |
| `emanate-test`                 | Records and checks `EmaViewModel` and `EmaSource` in your unit tests. See [Testing](guides/testing.md).                                           |

## Initialize Emanate

Call `Emanate.init` once, when the application starts and before any ViewModel is created:

```kotlin
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Emanate.init(EmaConfiguration.Android(debug = BuildConfig.DEBUG))
    }
}
```

Register the application in `AndroidManifest.xml` (`android:name=".MyApplication"`). With `BuildConfig.DEBUG`, debug
builds log every action and fail at once with any exception, and release builds don't log and keep working.
See [Configuration](guides/configuration.md).

## Your first screen

A screen is made of a **state**, the **actions** the user can perform, the **events** it can emit and a **ViewModel**
that connects them. This is a counter.

```kotlin
data class CounterState(val count: Int = 0) : EmaState

sealed interface CounterAction : EmaAction {
    data object Increment : CounterAction
}

sealed interface CounterEvent : EmaEvent {
    data class LimitReached(val limit: Int) : CounterEvent
}

class CounterViewModel :
    EmaViewModel<CounterState, CounterAction, CounterEvent>(CounterState()) {

    override fun setup() = source {
        reducers {
            add<CounterAction> { action ->
                when (action) {
                    is CounterAction.Increment -> state.copy(count = state.count + 1)
                }
            }
        }
        sideEffects {
            add<CounterAction.Increment> {
                if (state.count == LIMIT) publish(CounterEvent.LimitReached(LIMIT))
            }
        }
    }

    private companion object {
        const val LIMIT = 10
    }
}
```

- The **reducer** returns the next state for each action, and the view is notified.
- The **side effect** runs after the reducer, with the new state, and `publish` sends a one-shot event to the view.
- The ViewModel never touches platform classes, so it can be tested as plain Kotlin and shared between platforms.

### The screen

A Compose screen is described by an `EmaScreenContent`:

```kotlin
class CounterScreenContent : EmaScreenContent<CounterState, CounterAction> {

    @Composable
    override fun onState(
        state: CounterState,
        actions: EmaActionDispatcher<CounterAction>
    ) {
        Column {
            Text(text = state.count.toString())
            Button(onClick = { actions.dispatch(CounterAction.Increment) }) {
                Text(text = "+")
            }
        }
    }
}
```

And it is added to the navigation graph of an activity, where its events are handled:

```kotlin
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            NavHost(
                navController = navController,
                startDestination = CounterScreenContent::class.routeId
            ) {
                emaScreen(
                    screenContent = CounterScreenContent(),
                    viewModel = { CounterViewModel() },
                    onEvent = { event ->
                        when (event) {
                            is CounterEvent.LimitReached -> Toast.makeText(
                                this@MainActivity,
                                "Limit: ${event.limit}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
            }
        }
    }
}
```

`viewModel` is a lambda that creates the ViewModel. Use your dependency injection framework there, or create it
directly as above: see [Dependency injection](guides/dependency-injection.md). `onEvent` receives every event of the
screen, to show a message, as here, or to navigate. More in [Compose screens](compose/screens.md).

### The preview

`onState` only draws the state it receives, so a preview is one call: write it inside the screen content, give it
any state and an `EmaActionDispatcher.EMPTY`, which ignores the actions. No ViewModel, no fake data source:

```kotlin
class CounterScreenContent : EmaScreenContent<CounterState, CounterAction> {

    // onState, as above

    @Preview
    @Composable
    private fun PreviewCounter() {
        onState(state = CounterState(count = 7), actions = EmaActionDispatcher.EMPTY)
    }
}
```

The IDE creates the screen content to draw the preview, so keep its constructor without parameters. Add one preview
per interesting state (empty, loading, an error dialog...) to see every case of the screen at once.

## What just happened

1. The user taps the button: the screen calls `actions.dispatch(Increment)`.
2. The reducer of the ViewModel computes the next state.
3. The screen receives the new state and recomposes.
4. On the tenth tap the side effect also publishes `LimitReached`, which the screen shows once and consumes.

Continue with [Why Emanate](why-emanate.md) to see what else the same pieces can do, or with
[Architecture](concepts/architecture.md).
