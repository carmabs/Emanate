# Broadcasts

A broadcast **sends an action from one screen to other screens**. The typical case is a result: the profile screen
creates a friend, and the home screen, which is still in the back stack, adds it to its list.

![A broadcast reaches the ViewModels that listen to its type](../assets/broadcast.svg)

Any ViewModel that listens to the type of the action receives it as **one more action of its own**, so it handles it
with its usual [middlewares](middlewares.md), [reducers](reducers.md) and [side effects](side-effects.md). There are no
shared ViewModels, no result APIs of the platform and no casts: the result is a typed data class.

## In four steps

**1. Declare the action**, next to the actions of the screen that sends it:

```kotlin
sealed interface ProfileBroadcast : EmaAction {
    data class FriendCreated(val friend: User) : ProfileBroadcast
}
```

**2. Send it** from a side effect of the sender, with `broadcast`:

```kotlin
// ProfileViewModel
sideEffects {
    add<ProfileAction.SaveClicked> {
        broadcast(ProfileBroadcast.FriendCreated(state.friend))
        postEvent(ProfileEvent.Saved)  // The navigator closes the screen
    }
}
```

**3. Listen to its type** in the `broadcasts` block of the receiver:

```kotlin
// HomeViewModel
override fun setup() = source {
    broadcasts {
        listen<ProfileBroadcast.FriendCreated>()
    }
    // ...
}
```

**4. Handle it** like any other action:

```kotlin
// HomeViewModel
reducers {
    add<ProfileBroadcast.FriendCreated> { action -> state.copy(friends = state.friends + action.friend) }
}
sideEffects {
    add<ProfileBroadcast.FriendCreated> { action -> postEvent(HomeEvent.FriendAdded(action.friend)) }
}
```

> **Don't forget `listen`.** Without it, the reducers and side effects of that type never run, and nothing warns you
> at runtime. `assertSetup()` of [emanate-test](../guides/testing.md#checking-the-setup) catches it in a test.

## How it is delivered

- **At once**, to every ViewModel that listens to the type at that moment. The ViewModels of the previous screens are
  alive in the back stack, so they receive it.
- **To any number of receivers.** Every ViewModel that listens to the type receives the action.
- **Only while the receiver exists.** Its listener is removed when it is closed, with the rest of its work.
- **In memory.** It is not saved if the process is killed.
- **Lost if nobody listens**, unless it is kept. See below.

## Keeping it until it is received

With `keepUntilReceived = true`, an action that nobody is listening to is kept and delivered to the **first** ViewModel
that listens to its type. Only the last one of each class is kept. If someone is listening, it is delivered at once, as
usual.

```kotlin
broadcast(ProfileBroadcast.FriendCreated(state.friend), keepUntilReceived = true)
```

It covers a screen that doesn't exist yet. For example, when the system kills the app in the background, it restores
only the current screen: the ViewModel of the previous one is created when the user goes back to it. Without keeping
the result, it would be lost.

> If the receiver shows something, post an event: it waits until the view consumes it, so it is shown when the screen
> is visible again.

## Who can receive it

The actions go through the `broadcaster` of the [configuration](../guides/configuration.md). By default every
ViewModel of the app shares the same one. A library built with Emanate can give its ViewModels their own broadcaster,
so they only talk to each other. See [Libraries and SDKs](../guides/configuration.md#libraries-and-sdks).

## Testing it

Several ViewModels can be tested together: create the sender and the receiver, dispatch the action that broadcasts the
result in the first one and check the state of the second one.

In tests, `resetTestDispatchers` of `emanate-test` removes the kept actions of the shared broadcaster that nobody
received. Each `testConfiguration` has its own broadcaster, so its kept actions don't reach other tests.
