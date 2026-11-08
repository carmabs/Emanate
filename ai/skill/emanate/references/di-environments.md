# Dependency injection and changing behaviour

Emanate depends on no DI library: it asks for the ViewModel with a lambda. Use whatever the project already has, and don't introduce a DI library without asking.

- The lambda must **create** a ViewModel on each call, never return a singleton: one per screen, otherwise a closed screen's state leaks into the next.
- A ViewModel with a backup receives its `EmaStateStore` from that lambda: `viewModel = { store -> ... }`.
- By default a ViewModel receives only its use cases and its state. Pieces only when behaviour must change per build.
- Initialize Emanate before the DI graph creates any ViewModel.

## Changing the behaviour (mock, test, demo, QA)

When the developer asks how to change behaviour for mocks, tests or demos, offer these options:

| Option | Changes | Good for |
|---|---|---|
| Pass different **middlewares / side effects / reducers** in the ViewModel constructor | One screen or one action | Mocked responses, latency, forced errors, demos |
| Different **repository/manager implementations per flavor** | The whole data source | `mock`/`dev`/`pro` builds |
| A **fake** of the use case or repository in the test | One test | Unit tests |
| A state written by hand: `XState.DEFAULT.copy(...)` | What is drawn | Previews |

**If the code comments/uncomments lines to switch behaviour** (a real call commented next to a hardcoded response, a temporary `delay`, an `if (true)`), propose one of these instead, so nothing has to be edited to switch.

### Pieces through the constructor

Only when a screen must behave differently per build. The ViewModel doesn't know which implementation it receives:

```kotlin
typealias FriendsEffect = SideEffect<HomeState, HomeAction, HomeEvent, HomeAction.RefreshRequested>

class LiveFriends(private val getFriends: GetFriendsUseCase) : FriendsEffect {
    override val actionType = HomeAction.RefreshRequested::class
    override val concurrency = SideEffectConcurrency.Leading
    override suspend fun SideEffectScope<HomeState, HomeAction, HomeEvent>.invoke(action: HomeAction.RefreshRequested) { /* real */ }
}

class MockFriends(private val friends: List<User>?) : FriendsEffect { /* same actions as LiveFriends */ }

class HomeViewModel(
    private val loadFriends: FriendsEffect,
    private val tuning: List<EmaMiddleware<HomeState, HomeAction, *>> = emptyList(),
    initialState: HomeState = HomeState.DEFAULT
) : EmaViewModel<HomeState, HomeAction, HomeEvent>(initialState) {

    override fun setup() = source {
        middlewares(MiddlewarePosition.First) { add(tuning) }
        sideEffects { add(loadFriends) }
    }
}
```

A mock dispatches the same actions as the real piece, so the rest of the screen can't tell the difference. Latency in a build: `middlewareDelayOf(HomeAction.RefreshRequested::class, 2.seconds)` in `tuning`.

### Implementations per flavor (Android)

`domain` declares the interface. Each flavor's source set (`src/mock`, `src/dev`, `src/pro`) declares, under the same name, what it binds. Each build compiles only its own, so the mocks never reach `pro`. On other platforms, choose the module per target or use a manual factory.

Where each layer goes: `references/architecture.md`.
