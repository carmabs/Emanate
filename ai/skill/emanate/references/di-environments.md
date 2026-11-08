# Dependency injection and environments

Emanate has no DI dependency: it asks for the ViewModel with a lambda. Use Koin, a manual container or constructors.

- The lambda must **create** a ViewModel each call (Koin `factory`, never `single`): one per screen, otherwise a closed screen's state leaks into the next.
- Repositories: `single`. Use cases: `factory`. Initial state can be injected too (`factory { HomeState() }`).
- Start DI **after** `Emanate.init(...)`.

```kotlin
val uiModule = module {
    factory { (stateStore: EmaStateStore) -> HomeViewModel(get(), stateStore) }  // with backup
    factoryOf(::HomeViewModel)                                                    // without
}
```

## Injecting pieces (mock / dev / pro / QA / tests)

The ViewModel can receive middlewares, reducers and side effects, so DI decides how the whole screen behaves with no change to the ViewModel:

```kotlin
typealias FriendsEffect = SideEffect<HomeState, HomeAction, HomeEvent, HomeAction.RefreshRequested>

class LiveFriends(private val getFriends: GetFriendsUseCase) : FriendsEffect {
    override val actionType = HomeAction.RefreshRequested::class
    override val concurrency = SideEffectConcurrency.Leading
    override suspend fun SideEffectScope<HomeState, HomeAction, HomeEvent>.invoke(action: HomeAction.RefreshRequested) { /* real */ }
}
class MockFriends : FriendsEffect { /* fixed data */ }          // demo build
// QA: a middleware with delay(...) adds latency; a side effect that dispatches Failed shows the error state

class HomeViewModel(private val friends: FriendsEffect, initialState: HomeState) : EmaViewModel<...>(initialState) {
    override fun setup() = source { sideEffects { add(friends) } }
}
```

Per environment bind a different implementation (flavors or separate Koin modules): app = real, preview = fake sample data, QA = failing/slow fake, unit test = fake.

## Module layout

`app` (Application + DI, sees all) -> `ui` -> `presentation` (pure Kotlin) -> `domain` (pure Kotlin, repository **interfaces**, use cases) <- `data` (implementations).
`api` only when a dependency's types appear in your public signatures; otherwise `implementation`.
