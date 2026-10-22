# Initializers: passing data to a screen

An **initializer** is the data a screen needs to start. It travels in the route of the destination and reaches the
ViewModel as an action, **once**, the first time the screen is created.

## Declare it

```kotlin
@Serializable
sealed interface HomeInitializer : EmaInitializer {

    @Serializable
    data class HomeUser(val user: User) : HomeInitializer
}
```

A `sealed` type lets one screen start in different ways. A screen that receives nothing gets `EmaInitializer.EMPTY`.

## Use it in the ViewModel

The initializer is one more action: a reducer applies its data to the state, and a side effect loads what the screen
needs.

```kotlin
override fun setup() = source {
    reducers {
        add<HomeInitializer> { initializer ->
            when (initializer) {
                is HomeInitializer.HomeUser -> state.copy(
                    userData = HomeState.UserData(initializer.user.name, initializer.user.surname, initializer.user.role)
                )
            }
        }
    }
    sideEffects {
        add<HomeInitializer> { initializer ->
            when (initializer) {
                is HomeInitializer.HomeUser -> {
                    val friends = getUserFriendsUseCase(GetUserFriendsUseCase.Input(initializer.user))
                    dispatch(HomeFriendsLoaded(friends))
                }
            }
        }
    }
}
```

It arrives once, so a rotation doesn't apply it nor load the data again. After the death of the process, the screen
receives it again, and a [backup](process-death.md) restores what the user typed on top of it.

## Send and receive it

The initializer travels as text inside the route, serialized with kotlinx.serialization: annotate it with
`@Serializable` and any character is allowed in its values.

- The screen that navigates sends it with `NavController.navigate(route, initializerBundle)`.
- The destination declares how to read it with `initializerSupport`.

```kotlin
// Send
navController.navigate(
    route = ProfileCreationScreenContent::class.routeId,
    initializerBundle = EmaInitializerBundle(
        ProfileCreationInitializer.Admin,
        BundleSerializerStrategy.kSerialization(ProfileCreationInitializer.serializer())
    )
)

// Receive
createComposableScreen(
    screenContent = ProfileCreationScreenContent(),
    viewModel = { store -> get<ProfileCreationViewModel> { parametersOf(store) } },
    initializerSupport = EmaInitializerSupport.kSerialization(ProfileCreationInitializer.serializer()),
    onEvent = { navigator.handleProfileCreationEvent(it) }
)
```

The details, including the first screen of an activity, are in
[Compose navigation](../compose/navigation.md#passing-an-initializer).

## Testing it

In a test, call `onCreated` with the initializer, as the screen does:

```kotlin
viewModel.onCreated(HomeInitializer.HomeUser(ana))
```
