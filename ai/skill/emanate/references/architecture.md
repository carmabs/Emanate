# Architecture: layers and modules

A layer is normally a **Gradle module**: it shortens build times and the compiler enforces which code can see which.
`XXX` is the platform the app is designed for (`ui-android`, `data-android`, `utils-android`). Name the platform modules this way by default.
Minimum: `domain`, `data-XXX`, `presentation` and `app`.

| Layer | Contains | May use |
|---|---|---|
| `domain` | Models of the app, **interfaces** of repositories, managers and any data source, and the use cases. | Pure Kotlin only, no framework, so it works on any platform. `emanate-core` is multiplatform and allowed. |
| `data-XXX` | The **implementations** of those interfaces. | Framework and third-party libraries as needed. |
| `presentation` | Everything about drawing: composables, ViewModels, state, actions, events. | See the recommended split below. |
| `app` | Only dependency injection and library initialization. | Every module. |

## Recommended: split presentation in two

| Module | Contains |
|---|---|
| `presentation` | **Pure Kotlin**: ViewModels, state, actions, events, initializers. Reusable on several platforms. |
| `ui-XXX` | Only what draws and is specific to the platform: composables, screen contents, navigators, resources. |

## Optional: `utils-XXX`

Resources and functions of one platform shared by several modules.

## Several platforms in one project

One module per platform (`data-android`, `data-ios`, `ui-android`, `ui-ios`). A module shared by several platforms has no suffix.

## Dependencies

| Module | Knows |
|---|---|
| `app` | All the others |
| `presentation` | `domain` |
| `ui-XXX` | `presentation`, `utils-XXX`, `domain` |
| `data-XXX` | `utils-XXX`, `domain` |
| `utils-XXX` | `domain` |
| `domain` | Nothing of the app |

Every module lists what it knows explicitly, so `implementation` is enough.

## Emanate artifacts per module

Always `implementation`, never `api`: each module declares the artifacts it uses.

| Module | Artifacts |
|---|---|
| `domain` | `emanate-core` |
| `presentation` | `emanate-core`, `emanate-serialization` if it has a backup, `emanate-test` for tests |
| `ui-XXX` | `emanate-core`, `emanate-compose-android`, `emanate-compose-android-nav2` with a NavHost, `emanate-android` if it uses its helpers |
| `data-XXX`, `utils-XXX` | `emanate-core` only if they use its types |
| `app` | `emanate-core`, `emanate-android` |

## Initializing libraries

`app` initializes the injector and binds the implementations. To stay decoupled from a concrete library:

1. If a library needs initializing, do it inside the `data-XXX` implementation, when the concrete class that needs it is created.
2. If it is better to initialize it explicitly, add an `initialize()` to the interface of the repository or manager and call that from `app`.

```kotlin
// domain
interface AnalyticsManager {
    fun initialize()
    fun track(event: AnalyticsEvent)
}

// data-XXX: the only place that knows the library
class ThirdPartyAnalyticsManager(private val client: AnalyticsClient) : AnalyticsManager { /* ... */ }
```

## Organizing inside a module

One package per feature, with the same name in `presentation` and `ui-XXX`, not packages by type (`states/`, `viewmodels/`).
Deleting a screen means deleting those two folders.

```
presentation/.../home/   HomeState, HomeAction, HomeEvent, HomeDialog, HomeViewModel, HomeInitializer
ui-XXX/.../home/         HomeScreenContent
```
