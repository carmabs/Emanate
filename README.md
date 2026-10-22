# Emanate

**English** · [Español](README.es.md)

**Emanate** is a unidirectional architecture for Kotlin where every behaviour of a screen is a small piece you can
swap, intercept, reuse and test on its own: a **state** the view renders, **actions** the user performs, one-shot
**events** the view reacts to, and a ViewModel that handles each action with **middlewares**, **reducers** and
**side effects**. The ViewModels are pure Kotlin Multiplatform code and the screens are built with Jetpack Compose.

Because the pieces are values, the same screen can call the real API in production, answer with mocked data in a demo
build, simulate latency or errors in QA and run against a fake in the tests, only by changing what is injected.

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
                    CounterAction.Increment -> state.copy(count = state.count + 1)
                }
            }
        }
        sideEffects {
            add<CounterAction.Increment> {
                if (state.count == 10) postEvent(CounterEvent.LimitReached(10))
            }
        }
    }
}
```

## Install

```kotlin
// settings.gradle.kts
maven { url = uri("https://jitpack.io") }
```

```kotlin
dependencies {
    implementation("com.github.carmabs.ema:emanate-android:7.0.0")
    implementation("com.github.carmabs.ema:emanate-compose:7.0.0")
}
```

For a pure Kotlin or multiplatform module use `emanate-core`, and `emanate-test` to test the ViewModels.
Then initialize Emanate in your `Application`:

```kotlin
Emanate.init(EmaConfiguration.Android(debug = BuildConfig.DEBUG))
```

## Documentation

The guide is in [`docs/`](docs/index.md):

- [Why Emanate](docs/why-emanate.md) and [getting started](docs/getting-started.md)
- Concepts: [architecture](docs/concepts/architecture.md), [state](docs/concepts/state.md), [actions](docs/concepts/actions.md), [events](docs/concepts/events.md), [ViewModel](docs/concepts/viewmodel.md), [source](docs/concepts/source.md)
- Guides: [initialization and configuration](docs/guides/configuration.md), [surviving process death](docs/guides/process-death.md), [dependency injection and environments](docs/guides/dependency-injection.md), [testing](docs/guides/testing.md), [Kotlin Multiplatform](docs/guides/multiplatform.md) and more
- [Compose](docs/compose/screens.md): screens, navigation, back handling, dialogs and permissions
- [Recommendations](docs/recommendations.md) and [contributing](docs/contributing.md)

## Sample

The [`sample/`](sample) app shows every feature working together: a login with validation and dialogs,
a list with roles, and a Compose flow with navigation, back handling and saved state.
Open the `sample` folder in Android Studio and run the `app` module.
