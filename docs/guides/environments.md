# Mocking and environments

The behaviour of an Emanate screen is a list of pieces (middlewares, reducers and side effects), and
[every piece can be replaced](../concepts/source.md#pieces-are-values). A ViewModel that **receives** a piece instead of
declaring it doesn't know which one it gets, so the same screen can:

- call the real backend in production,
- answer with mocked data in a demo build, with no backend at all,
- simulate latency or errors in a QA build,
- and run against a fake in the unit tests,

**only by changing what is injected**. The ViewModel, the screen and the rest of the app stay exactly the same.

![The same ViewModel in three environments](../assets/environments.svg)

Without Emanate, these environments usually mean an `if (isDemo)` inside the ViewModel, a fake implementation of a
whole repository, or a mocking library that only works in the tests. Here you replace exactly the behaviour you want
to change, at the size you want: one response, one delay, one rule.

## 1. Make a piece injectable

The home screen loads the friends of the user. Take the side effect that loads them out of the ViewModel, as a class
that implements `SideEffect`. A type alias keeps the signatures short:

```kotlin
typealias FriendsEffect = SideEffect<HomeState, HomeAction, HomeEvent, HomeAction.RefreshRequested>

class LiveFriends(private val getFriendsUseCase: GetFriendsUseCase) : FriendsEffect {

    override val actionType = HomeAction.RefreshRequested::class
    override val concurrency = SideEffectConcurrency.Leading

    override suspend fun SideEffectScope<HomeState, HomeAction, HomeEvent>.invoke(action: HomeAction.RefreshRequested) {
        dispatch(HomeAction.EffectQueryResult.Started)
        getFriendsUseCase(GetFriendsUseCase.Input(state.query))
            .onSuccess { friends -> dispatch(HomeAction.EffectQueryResult.Loaded(friends)) }
            .onFailure { dispatch(HomeAction.EffectQueryResult.Failed) }
    }
}
```

The ViewModel receives it and installs it with `+`. Middlewares can be received the same way:

```kotlin
class HomeViewModel(
    private val loadFriends: FriendsEffect,
    private val tuning: List<EmaMiddleware<HomeState, HomeAction, *>> = emptyList()
) : EmaViewModel<HomeState, HomeAction, HomeEvent>(HomeState()) {

    override fun setup() = source {
        // Before anything else, so they see every action first
        middlewares(MiddlewarePosition.First) {
            tuning.forEach { +it }
        }
        reducers {
            add<HomeAction> { action ->
                when (action) {
                    is HomeAction.EffectQueryResult.Started -> state.copy(isLoading = true)
                    is HomeAction.EffectQueryResult.Loaded -> state.copy(isLoading = false, friends = action.friends)
                    is HomeAction.EffectQueryResult.Failed -> state.copy(isLoading = false, dialog = HomeDialog.LoadError)
                    // ...
                }
            }
        }
        sideEffects {
            +loadFriends
            add<HomeAction.FriendClicked> { action -> postEvent(HomeEvent.FriendSelected(action.friend)) }
        }
    }
}
```

The reducers and the events don't change between environments: only the source of the data does. The screen still
shows its loading state, its error dialog and its navigation exactly as in production.

## 2. Mock a backend response

A mock is one more implementation of the same piece. It dispatches the same actions as the real one, so the rest of the
screen can't tell the difference:

```kotlin
class MockFriends(
    private val friends: List<User>?,
    private val latency: Duration = 500.milliseconds
) : FriendsEffect {

    override val actionType = HomeAction.RefreshRequested::class
    override val concurrency = SideEffectConcurrency.Leading

    override suspend fun SideEffectScope<HomeState, HomeAction, HomeEvent>.invoke(action: HomeAction.RefreshRequested) {
        dispatch(HomeAction.EffectQueryResult.Started)
        delay(latency)
        dispatch(if (friends != null) HomeAction.EffectQueryResult.Loaded(friends) else HomeAction.EffectQueryResult.Failed)
    }
}
```

`MockFriends(friends = null)` shows the error dialog, `MockFriends(emptyList())` the empty list and
`MockFriends(hundredFriends)` a long one. A sales demo, a screenshot test or a designer reviewing the flow don't need a
backend anymore.

## 3. Simulate latency

`middlewareDelayOf` delays the actions of a type before they reach the reducers and the side effects. Inject it in a QA
build to see every loading state as a slow network would show it:

```kotlin
val slowRefresh = middlewareDelayOf<HomeState, HomeAction, HomeAction.RefreshRequested>(
    HomeAction.RefreshRequested::class,
    2.seconds
)
```

With `nextAfter`, a middleware can also decide the delay, for example a random one for every action:

```kotlin
val randomLatency = middlewareOf<HomeState, HomeAction, EmaAction>(EmaAction::class) {
    nextAfter { delay(Random.nextLong(200, 1500)) }
}
```

## 4. Simulate errors

A middleware can replace an action with `next(action)`. This one turns every refresh into a failure, so the error
dialog can be checked in a build that talks to the real backend:

```kotlin
val failingRefresh = middlewareOf<HomeState, HomeAction, HomeAction.RefreshRequested>(HomeAction.RefreshRequested::class) {
    next(HomeAction.EffectQueryResult.Failed)
}
```

And one that doesn't call `next` stops the action: it never reaches the reducers nor the side effects. For example, to
keep a feature disabled in a build:

```kotlin
val profilesDisabled = middlewareOf<HomeState, HomeAction, HomeAction.FriendClicked>(HomeAction.FriendClicked::class) {
    // Not calling next: the profiles can't be opened in this build
}
```

## 5. Replace a rule

Reducers can be replaced too. A demo build can give every user the administrator role, to show the screens that only
administrators see, without a special account:

```kotlin
class HomeViewModel(
    private val userReducer: EmaReducer<HomeState, HomeInitializer> = reducerOf(HomeInitializer::class) { initializer ->
        when (initializer) {
            is HomeInitializer.HomeUser -> state.copy(user = initializer.user)
        }
    }
) : EmaViewModel<HomeState, HomeAction, HomeEvent>(HomeState()) {

    override fun setup() = source {
        reducers { +userReducer }
    }
}

// Demo build
HomeViewModel(
    userReducer = reducerOf(HomeInitializer::class) { initializer ->
        when (initializer) {
            is HomeInitializer.HomeUser -> state.copy(user = initializer.user.copy(role = Role.ADMIN))
        }
    }
)
```

## 6. Open a screen in a given state

For QA, open a screen already in the state you want to check, without repeating the steps that lead to it. An
extension function adds a side effect that dispatches a list of actions when the screen is created:

```kotlin
fun <S : EmaState, A : EmaAction, E : EmaEvent> SourceSetupScope<S, A, E>.startWith(actions: List<A>) {
    sideEffects {
        add<EmaInitializer> { actions.forEach { dispatch(it) } }
    }
}

class HomeViewModel(
    private val loadFriends: FriendsEffect,
    private val startActions: List<HomeAction> = emptyList()
) : EmaViewModel<HomeState, HomeAction, HomeEvent>(HomeState()) {

    override fun setup() = source {
        startWith(startActions)
        // ...
    }
}

// QA build: a search with no results
HomeViewModel(
    loadFriends = MockFriends(friends = emptyList()),
    startActions = listOf(HomeAction.QueryWritten("Zoe"), HomeAction.RefreshRequested)
)
```

The actions go through the same reducers as the ones of the user, so the state is always a valid one.

## 7. Choose the pieces by environment

Decide the pieces where the ViewModel is created: in your dependency injection. Declare the production pieces in the
main source set and the ones of each environment in the source set of its build type or flavor, so the mocks never
reach the release build. With Koin:

```kotlin
// main: production
val homeModule = module {
    factory<FriendsEffect> { LiveFriends(get()) }
    factory { HomeViewModel(loadFriends = get()) }
}

// demo: no backend
val environmentModule = module {
    factory<FriendsEffect> { MockFriends(friends = demoFriends) }
}

// qa: the real backend, slow
val environmentModule = module {
    factory {
        HomeViewModel(
            loadFriends = LiveFriends(get()),
            tuning = listOf(middlewareDelayOf(HomeAction.RefreshRequested::class, 2.seconds))
        )
    }
}
```

```kotlin
startKoin {
    // The module of the environment is loaded last, so its definitions replace the ones of production
    modules(dataModule, useCaseModule, homeModule, environmentModule)
}
```

The release source set has an empty `environmentModule`. The same works with any other dependency injection framework,
or with a manual factory. See [Dependency injection](dependency-injection.md).

## 8. The same pieces in the tests

The tests create the ViewModel with the pieces they need. No mocking library and no device:

```kotlin
@Test
fun `a failed load shows the error dialog`() = runTest {
    val viewModel = HomeViewModel(loadFriends = MockFriends(friends = null))
    val recorder = viewModel.record()

    viewModel.dispatch(HomeAction.RefreshRequested)
    advanceUntilIdle()

    recorder.assertState { it.dialog == HomeDialog.LoadError && !it.isLoading }
}

@Test
fun `a slow load keeps the loading state until it finishes`() = runTest {
    val viewModel = HomeViewModel(loadFriends = MockFriends(friends = listOf(ana), latency = 3.seconds))
    val recorder = viewModel.record()

    viewModel.dispatch(HomeAction.RefreshRequested)
    advanceTimeBy(2.seconds)
    recorder.assertState { it.isLoading }

    advanceUntilIdle()
    recorder.assertState { it.friends == listOf(ana) }
}
```

The delays run in virtual time, so a test of a three-second request takes milliseconds. See [Testing](testing.md).

## Guidelines

- **Swap the smallest piece.** Replace the side effect that calls the backend, not the ViewModel. The reducers, the
  events and the screen are then tested and demoed exactly as they run in production.
- **Mocks dispatch the same actions as the real pieces.** Then the rest of the screen can't tell them apart.
- **Keep the mocks out of the release build**, in the source set of their build type or flavor.
- **Use `MiddlewarePosition.First`** for the middlewares that simulate conditions, so they run before the ones of the
  screen.
- **Prefer a plugin for a whole feature** that an environment installs or not, like analytics or a debug overlay. See
  [Plugins](../concepts/source.md#plugins).
