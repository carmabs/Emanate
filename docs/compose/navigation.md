# Compose navigation

In Emanate a **ViewModel never navigates**. Navigating is the job of the navigation graph that contains the screen,
usually the `NavHost`: it is the one that knows which screens exist and holds the `NavController` to move between them.

The ViewModel only posts an [event](../concepts/events.md) that says **what happened**, like "the user chose to be an
admin". Someone outside the screens has to receive those events and decide **where to go**: the **navigator**, a class
that lives next to the `NavHost` and holds its `NavController`. It is not a class of Emanate: it is a plain class of
yours, and it is a good pattern to handle all the navigation of your app this way.

This is the profile flow of the sample, with two screens:

![Each screen of the NavHost sends its events to the navigator, which decides where to go](../assets/compose-navigation.svg)

1. Each destination of the `NavHost` is an `emaScreen`, with its screen content, its ViewModel and its `onEvent`.
2. When the user taps **Admin**, the ViewModel of `ProfileOnBoarding` posts `UserTypeSelected(ADMIN)`. It doesn't
   know which screen comes next.
3. The `onEvent` of that screen hands the event to the navigator, to its function for that screen:
   `handleProfileOnBoardingEvent`.
4. The navigator decides for each event: `UserTypeSelected` opens `ProfileCreation`, and `OnBoardingCancelled` goes
   back. It does it with the `navController`, which changes the destination shown by the `NavHost`.

Keeping the navigation out of the ViewModel has two advantages. The ViewModel can be tested without Android, and the
same event can lead to different places in different apps or graphs: on a phone it may open a new screen and on a
tablet update a side panel.

The decision of where to go could be written directly in each `onEvent` lambda, but we recommend that every navigation
goes through a navigator: the graph only declares the screens, and all the navigation of a flow lives in one place that
is easy to read and to change. See [The navigator](#the-navigator).

## Routes

Every screen registered with `emaScreen` or `emaDialog` has a route. By default it is `ScreenContent::class.routeId`,
unique per class:

```kotlin
navController.navigate(ProfileCreationScreenContent::class.routeId)
```

## The navigator

Create **one navigator per navigation graph**, not one per screen, and let it handle the navigation of all the screens
of the graph. It is a plain class that receives the `NavController` and has one function per screen: each function
receives the events of that screen and decides where to go. Emanate doesn't require a base class or an interface for it:
any class with this shape works. Create it once, with `remember`, next to the `NavHost`:

```kotlin
class ProfileNavigator(private val navController: NavController) {

    fun handleProfileOnBoardingEvent(event: ProfileOnBoardingEvent) {
        when (event) {
            is ProfileOnBoardingEvent.UserTypeSelected -> navController.navigate(
                route = ProfileCreationScreenContent::class.routeId,
                initializerBundle = EmaInitializerBundle(
                    mapToCreationInitializer(event.role),
                    BundleSerializerStrategy.kSerialization(ProfileCreationInitializer.serializer())
                )
            )

            is ProfileOnBoardingEvent.OnBoardingCancelled -> navController.navigateBack()
        }
    }

    fun handleProfileCreationEvent(event: ProfileCreationEvent) {
        when (event) {
            ProfileCreationEvent.DialogConfirmationAccepted -> navController.navigateBack()
        }
    }
}
```

```kotlin
setContent {
    val navController = rememberNavController()
    val navigator = remember { ProfileNavigator(navController) }

    NavHost(navController, startDestination = ProfileOnBoardingScreenContent::class.routeId) {
        emaScreen(
            screenContent = ProfileOnBoardingScreenContent(),
            viewModel = { get<ProfileOnBoardingViewModel>() },
            onEvent = { navigator.handleProfileOnBoardingEvent(it) }
        )
    }
}
```

`NavController.navigateBack(closeActivityWhenBackstackIsEmpty = true)` pops the back stack and closes the activity when
there is nothing left, and `NavController.navigateToExternalLink(url)` opens a link in another app.

## Passing an initializer

Send the [initializer](../guides/initializers.md) with the `navigate` extension that receives an `EmaInitializerBundle`:

```kotlin
navController.navigate(
    route = ProfileCreationScreenContent::class.routeId,
    initializerBundle = EmaInitializerBundle(
        ProfileCreationInitializer.Admin,
        BundleSerializerStrategy.kSerialization(ProfileCreationInitializer.serializer())
    )
)
```

Another overload receives a `NavOptionsBuilder` lambda, like `navigate(route) { launchSingleTop = true }`.

The destination declares how to read it with `initializerSupport`. The initializer travels in the route as text, so
Compose navigation reads it with kotlinx.serialization: `EmaInitializerSupport` only accepts the `kSerialization`
strategy, and the `parcelable` and `serializable` strategies, which only work with Bundles, fail if they are used in a
route:

```kotlin
emaScreen(
    screenContent = ProfileCreationScreenContent(),
    viewModel = { get<ProfileCreationViewModel>() },
    initializerSupport = EmaInitializerSupport.kSerialization(ProfileCreationInitializer.serializer()),
    onEvent = { navigator.handleProfileCreationEvent(it) }
)
```

The initializer reaches the ViewModel as an action, the first time the screen is created. See [Initializers](../guides/initializers.md).

### The first screen of an activity

The start destination is not reached with `navigate`, so it has no initializer in its route. When the activity is
started with an initializer in its intent, read it and pass it as `overrideInitializer`, which replaces the one of the route:

```kotlin
emaScreen(
    screenContent = ProfileOnBoardingScreenContent(),
    viewModel = { get<ProfileOnBoardingViewModel>() },
    initializerSupport = EmaInitializerSupport.kSerialization(
        ProfileOnBoardingInitializer.serializer(),
        getInitializer(
            BundleSerializerStrategy.kSerialization(ProfileOnBoardingInitializer.serializer()),
            savedInstanceState
        )
    ),
    onEvent = { navigator.handleProfileOnBoardingEvent(it) }
)
```
