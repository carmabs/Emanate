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
    implementation("com.github.carmabs.ema:emanate-android:$emanateVersion")
    implementation("com.github.carmabs.ema:emanate-compose:$emanateVersion")
}
```

For a pure Kotlin module, like a `presentation` module with the ViewModels, add only the core, and `emanate-test` to
test them:

```kotlin
dependencies {
    implementation("com.github.carmabs.ema:emanate-core:$emanateVersion")
    testImplementation("com.github.carmabs.ema:emanate-test:$emanateVersion")
}
```

`emanate-core` is a Kotlin Multiplatform library, so it can also be used from the `commonMain` of a multiplatform module.
See [Kotlin Multiplatform](guides/multiplatform.md).

## Modules

| Artifact                | Use it for                                                                                     |
|-------------------------|------------------------------------------------------------------------------------------------|
| `emanate-core`          | State, actions, events, ViewModels, sources, plugins and use cases. Pure Kotlin Multiplatform. |
| `emanate-compose`       | Jetpack Compose screens, navigation and previews.                                              |
| `emanate-android`       | The integration with the Android platform: configuration, initializers, permissions and the store of the saved state. |
| `emanate-serialization` | Saves any serializable value of the state across process death. Based on kotlinx.serialization. See the [backup plugin](guides/saving-state.md). |
| `emanate-test`          | Records and checks the ViewModels and sources in your unit tests. See [Testing](guides/testing.md). |

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
                if (state.count == LIMIT) postEvent(CounterEvent.LimitReached(LIMIT))
            }
        }
    }

    private companion object {
        const val LIMIT = 10
    }
}
```

- The **reducer** returns the next state for each action, and the view is notified.
- The **side effect** runs after the reducer, with the new state, and `postEvent` sends a one-shot event to the view.
- The ViewModel never touches platform classes, so it can be tested as plain Kotlin and shared between platforms.

### The screen

A Compose screen is described by an `EmaComposableScreenContent`:

```kotlin
class CounterScreenContent :
    EmaComposableScreenContent<CounterState, CounterAction, CounterEvent> {

    @Composable
    override fun onState(
        state: CounterState,
        actions: EmaImmutableActionDispatcher<CounterAction>
    ) {
        Column {
            Text(text = state.count.toString())
            Button(onClick = { actions.dispatch(CounterAction.Increment) }) {
                Text(text = "+")
            }
        }
    }

    override suspend fun onEvent(
        context: Context,
        event: CounterEvent,
        actions: EmaImmutableActionDispatcher<CounterAction>
    ) {
        when (event) {
            is CounterEvent.LimitReached ->
                Toast.makeText(context, "Limit: ${event.limit}", Toast.LENGTH_SHORT).show()
        }
    }
}
```

And it is added to the navigation graph of an activity:

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
                createComposableScreen(
                    screenContent = CounterScreenContent(),
                    viewModel = { CounterViewModel() },
                    onEvent = { event ->
                        // Navigation decisions go here
                    }
                )
            }
        }
    }
}
```

`viewModel` is a lambda that creates the ViewModel. Use your dependency injection framework there, or create it
directly as above: see [Dependency injection](guides/dependency-injection.md). More in [Compose screens](compose/screens.md).

### The preview

`onState` only draws the state it receives, so a preview is one call: write it inside the screen content, give it
any state and an `EmaImmutableActionDispatcher.EMPTY`, which ignores the actions. No ViewModel, no fake data source:

```kotlin
class CounterScreenContent :
    EmaComposableScreenContent<CounterState, CounterAction, CounterEvent> {

    // onState and onEvent, as above

    @Preview
    @Composable
    private fun CounterPreview() {
        onState(state = CounterState(count = 7), actions = EmaImmutableActionDispatcher.EMPTY)
    }
}
```

The IDE creates the screen content to draw the preview, so keep its constructor without parameters. Add one preview
per interesting state (empty, loading, an error dialog...) to see every case of the screen at once.

## What just happened

1. The user taps the button: the screen calls `actions.dispatch(Increment)`.
2. The reducer of the ViewModel computes the next state.
3. The screen receives the new state and recomposes.
4. On the tenth tap the side effect also posts `LimitReached`, which the screen shows once and consumes.

Continue with [Why Emanate](why-emanate.md) to see what else the same pieces can do, or with
[Architecture](concepts/architecture.md).
