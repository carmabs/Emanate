# Compose screens

Compose screens are described by an `EmaComposableScreenContent` and registered in a navigation graph with
`createComposableScreen`. They need the `emanate-android` and `emanate-compose` artifacts.

## Screen content

```kotlin
class ProfileOnBoardingScreenContent :
    EmaComposableScreenContent<ProfileOnBoardingState, ProfileOnBoardingActions, ProfileOnBoardingEvent> {

    @Composable
    override fun onState(
        state: ProfileOnBoardingState,
        actions: EmaImmutableActionDispatcher<ProfileOnBoardingActions>
    ) {
        // Draw the state. Send user input with actions.dispatch(...)
    }
}
```

`EmaComposableScreenContent<S, A, E>` has three members:

| Member                                      | Purpose                                                                  |
|---------------------------------------------|--------------------------------------------------------------------------|
| `onState(state, actions)`                   | **Required.** Draws the state. It is a `@Composable`.                    |
| `suspend onEvent(context, event, actions)`  | Handles [events](../concepts/events.md) that are UI concerns (messages, haptics...). Optional. |
| `onBack(state): A?`                         | The action to send when back is pressed, or `null`. See [Back handling](back-handling.md). Optional. |

Keep the screen content free of state: it is a stateless description, created once per screen.
Anything that must survive belongs in the [state](../concepts/state.md) of the ViewModel.

## Registering the screen

```kotlin
NavHost(
    navController = navController,
    startDestination = ProfileOnBoardingScreenContent::class.routeId
) {
    createComposableScreen(
        screenContent = ProfileOnBoardingScreenContent(),
        viewModel = { get<ProfileOnBoardingViewModel>() },
        onEvent = { navigator.handleProfileOnBoardingEvent(it) }
    )
}
```

| Parameter              | Purpose                                                                                        |
|------------------------|------------------------------------------------------------------------------------------------|
| `screenContent`        | The screen content.                                                                            |
| `viewModel`            | A lambda that creates the ViewModel. It is called once, when the screen is opened, and receives the `EmaStateStore` of the screen for a [backup](../guides/process-death.md). See [Dependency injection](../guides/dependency-injection.md). |
| `onEvent`              | Receives every event. Put **navigation** here, where the `NavController` is available.         |
| `routeId`              | The route. By default `ScreenContent::class.routeId`, which is unique per class.               |
| `initializerSupport`   | How to read the [initializer](../guides/initializers.md) from the route. See [Navigation](navigation.md#passing-an-initializer). |
| `transitionAnimation`  | `EmaComposableTransitions(enterTransition, exitTransition, popEnterTransition, popExitTransition)`. The pop transitions default to the enter and exit ones. |
| `fullScreenDialogMode` | Registers the destination as a `dialog` instead of a `composable`.                             |
| `decoration`           | Wraps the screen, for example in a `Scaffold`. Receives the content and the action dispatcher. |
| `previewRenderState`   | State to draw in the IDE preview.                                                              |
| `onViewModelInstance`  | Gives you the ViewModel instance, for example to share it with other composables.              |

## Two places to handle events

Each event reaches **both** handlers, in this order:

1. The `onEvent` lambda of `createComposableScreen` (no context, but your `NavController` is at hand).
2. `screenContent.onEvent(context, event, actions)`.

Then the event is consumed. A simple rule: navigation in the graph, everything else in the screen content.

## Lifecycle

The screen connects the ViewModel to the lifecycle of its destination:

| Lifecycle event                   | The ViewModel receives                          |
|-----------------------------------|-------------------------------------------------|
| `ON_CREATE`, the first time       | The `EmaInitializer` of the screen              |
| `ON_START`                        | `EmaLifecycle.Started`                          |
| `ON_RESUME`                       | `EmaLifecycle.Resumed`                          |
| `ON_PAUSE`                        | `EmaLifecycle.Paused`                           |
| `ON_STOP`                         | `EmaLifecycle.Stopped`                          |
| The screen leaves the composition | `EmaLifecycle.Paused` and `EmaLifecycle.Stopped` |
| The destination is removed        | `EmaLifecycle.Destroyed`, and it is closed      |

The state is collected with `collectAsStateWithLifecycle` and the events while the lifecycle is `STARTED`.

## Previews

Inside the IDE preview there is no ViewModel, so call `onState` directly with a state and an empty dispatcher:

```kotlin
@Preview
@Composable
private fun NormalPreview() {
    EmaSampleTheme {
        ProfileCreationScreenContent().onState(
            state = ProfileCreationState(Role.ADMIN, "Carlos", "Mateo"),
            actions = EmaImmutableActionDispatcher.EMPTY
        )
    }
}
```

`previewRenderState` in `createComposableScreen` does the same for the whole screen: in a preview, the ViewModel is not
created and the screen draws that state.

## Using a screen without a navigation graph

`EmaComposableScreen` is the composable behind `createComposableScreen`. Use it to place an Emanate screen anywhere:

```kotlin
EmaComposableScreen(
    vm = { CounterViewModel() },
    screenContent = CounterScreenContent(),
    onEvent = { /* ... */ }
)
```

It accepts the `initializer` and a `previewRenderState`. Another overload receives a ViewModel instance and its action dispatcher, for ViewModels
created and kept by you.

## Theme

Wrap the `NavHost` (or your content) in your Material theme. The sample uses `EmaSampleTheme`.
