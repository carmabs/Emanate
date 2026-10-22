# Testing

An `EmaViewModel` has no platform dependencies, so you test it as plain Kotlin, with no device and no emulator. And because
every change goes through its source, a test can see **everything** the screen did, in order: each action, each state
and each event.

| You control                                        | You check                                         |
|----------------------------------------------------|---------------------------------------------------|
| The actions, with `dispatch` and `onCreated`       | The states returned by the reducers, in order     |
| The pieces it receives: fakes, mocks, delays       | The events posted, in order                       |
| The time, with the virtual clock of `runTest`      | The actions, including the ones a middleware stopped |

## Testing an EmaViewModel with emanate-test

`emanate-test` has the utilities to test the ViewModels built with `EmaViewModel` and the
[sources](../concepts/source.md). Add it to the unit tests of the module of your ViewModels (for now it is published for
the JVM test source sets):

```kotlin
dependencies {
    testImplementation("com.github.carmabs.ema:emanate-test:$emanateVersion")
}
```

It brings `kotlinx-coroutines-test` and requires the same version of `emanate-core` it was built with.

| Utility                         | Purpose                                                                       |
|---------------------------------|-------------------------------------------------------------------------------|
| `useTestDispatchers()`          | Runs the ViewModels, sources and use cases created after it on the dispatcher of the test, also as `Dispatchers.Main`. Call it before each test. |
| `resetTestDispatchers()`        | Restores the dispatchers and the configuration of Emanate. Call it after each test. |
| `testConfiguration(dispatcher)` | A configuration that runs on the dispatcher of the test, to pass it to the ViewModels instead of calling `useTestDispatchers()`. See [Without changing the configuration of Emanate](#without-changing-the-configuration-of-emanate). |
| `viewModel.record()`            | Records in order the actions, states and events of a ViewModel or a source.  |
| `assertActions(...)`            | The recorded actions, including the ones blocked by a middleware.            |
| `assertStates(...)`             | Every state returned by the reducers.                                         |
| `assertState(state)` / `assertState { }` | The last state, or a condition on it.                               |
| `assertEvents(...)` / `assertNoEvents()` | The posted events.                                                  |
| `viewModel.assertSetup()`       | Fails if a middleware, reducer or side effect never runs, like the ones of a broadcast action that isn't listened to. See [Checking the setup](#checking-the-setup). |

```kotlin
class HomeViewModelTest {

    @BeforeTest
    fun setUp() {
        useTestDispatchers()
    }

    @AfterTest
    fun tearDown() {
        resetTestDispatchers()
    }

    @Test
    fun `a search shows the matching friends`() = runTest {
        val viewModel = HomeViewModel(GetFriendsUseCase(FakeRepository()), HomeState())
        val recorder = viewModel.record()

        viewModel.dispatch(HomeAction.QueryWritten("An"))
        viewModel.dispatch(HomeAction.RefreshRequested)
        advanceUntilIdle()

        recorder.assertState { !it.isLoading && it.friends == listOf(ana) }
        recorder.assertEvents(HomeEvent.FriendsUpdated(1))
    }
}
```

- **Create the ViewModel after `useTestDispatchers`**, so it runs on the dispatcher of the test. By default it is an
  `UnconfinedTestDispatcher`, which processes the actions as soon as they are dispatched; pass a
  `StandardTestDispatcher` to process them when the test advances.
- **Record before the first action** to record everything.
- **`advanceUntilIdle()`** runs the side effects, delays included, because `runTest` uses the scheduler of the test
  dispatcher.
- **Several ViewModels** can be tested together, for example the results broadcast between screens. See
  [Lifecycle, initializers and results](#lifecycle-initializers-and-results).

### Without changing the configuration of Emanate

`useTestDispatchers()` changes the configuration of Emanate and `Dispatchers.Main`, which are shared by the whole
process, so the tests that use it can't run in parallel in the same process. If your ViewModels receive the configuration in
the constructor (see [Explicit configuration](configuration.md#explicit-configuration)), pass them
`testConfiguration(dispatcher)` instead: it doesn't change anything shared, so there is nothing to reset.

```kotlin
class HomeViewModel(
    private val getFriendsUseCase: GetFriendsUseCase,
    initialState: HomeState,
    configuration: EmaConfiguration = Emanate.configuration
) : EmaViewModel<HomeState, HomeAction, HomeEvent>(initialState, configuration)

class HomeViewModelTest {

    @Test
    fun `a search shows the matching friends`() = runTest {
        val viewModel = HomeViewModel(
            GetFriendsUseCase(FakeRepository()),
            HomeState(),
            testConfiguration(UnconfinedTestDispatcher(testScheduler))
        )
        val recorder = viewModel.record()
        // ...
    }
}
```

- **Create the dispatcher with `testScheduler`**, the scheduler of `runTest`, so `advanceUntilIdle()` runs the work of
  the ViewModel. `Dispatchers.Main` isn't replaced, so `runTest` doesn't take it from there.
- The rest of the configuration is the default `EmaConfiguration()`, not the one of Emanate. Pass another one as the
  second parameter to change it: `testConfiguration(dispatcher, EmaConfiguration(logger = null))`.
- The [use cases](use-cases.md) run on the `io` dispatcher of the configuration of Emanate unless they receive
  another one in the constructor: fake them, or pass them the dispatcher of the test.
- **Each `testConfiguration` has its own broadcaster**, so the actions broadcast in a test don't reach the others. The
  ViewModels of a test that send results to each other must receive the same configuration: create it once.

### Checking the setup

A reducer, middleware or side effect for an action that the ViewModel never receives doesn't fail: it never runs. It
happens when a broadcast action is handled but `listen<Type>()` is missing in the `broadcasts` block. `assertSetup()`
checks that every piece can receive actions of its type: actions of the screen, initializers, lifecycle actions or
broadcast actions it listens to.

```kotlin
@Test
fun `the setup is complete`() {
    HomeViewModel(GetUserFriendsUseCase(FakeRepository()), HomeState.DEFAULT).assertSetup()
}
```

It fails with every piece that never runs:

```
The reducer of UserCreated never runs: HomeViewModel doesn't receive UserCreated. If it is broadcast by another
source, add listen<UserCreated>() in the broadcasts block.
```

Only the types that the ViewModel can't receive for sure are reported. An interface can be implemented by an action of
the screen, so a piece for an interface that isn't related to the actions of the screen is not reported.

## Swapping pieces in the tests

A ViewModel that [receives its pieces](dependency-injection.md#injecting-pieces) is tested with the pieces the test needs: a mock that answers
at once, one that fails, one that takes a while. No mocking library is needed, and the reducers and events under test
are the ones of production.

```kotlin
@Test
fun `a failed load shows the error dialog`() = runTest {
    val viewModel = HomeViewModel(loadFriends = MockFriends(friends = null))
    val recorder = viewModel.record()

    viewModel.dispatch(HomeAction.RefreshRequested)
    advanceUntilIdle()

    recorder.assertState { it.dialog == HomeDialog.LoadError && !it.isLoading }
    recorder.assertNoEvents()
}

@Test
fun `a second refresh while loading is ignored`() = runTest {
    val viewModel = HomeViewModel(loadFriends = MockFriends(friends = listOf(ana), latency = 1.seconds))
    val recorder = viewModel.record()

    viewModel.dispatch(HomeAction.RefreshRequested)
    viewModel.dispatch(HomeAction.RefreshRequested)
    advanceUntilIdle()

    recorder.assertActions(
        HomeAction.RefreshRequested, HomeAction.EffectQueryResult.Started, HomeAction.RefreshRequested, HomeAction.EffectQueryResult.Loaded(listOf(ana))
    )
}
```

### Time is virtual

The side effects and middlewares run on the dispatcher of the test, so their `delay`s use the virtual clock of
`runTest`. A debounce, a timeout or a three-second request are tested in milliseconds, and `advanceTimeBy` checks the
state at any point in between:

```kotlin
viewModel.dispatch(HomeAction.QueryWritten("an"))
viewModel.dispatch(HomeAction.QueryWritten("ana"))
advanceTimeBy(200.milliseconds)
recorder.assertNoEvents()          // still debouncing

advanceUntilIdle()
recorder.assertState { it.friends == listOf(ana) && it.query == "ana" }
```

The same goes for a mock with latency, to check the loading state of a slow request:

```kotlin
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

### Lifecycle, initializers and results

The lifecycle, the initializer and the results of other screens are actions too, so a test sends them the same way as
the screen does:

```kotlin
viewModel.onCreated(HomeInitializer.HomeUser(ana))   // EmaInitializer
viewModel.onResumeView()                             // EmaLifecycle.Resumed
```

Several ViewModels can be tested together, for example to check a result broadcast between screens: create the
profile and home ViewModels, add a friend in the first one and check that it reaches the list of the second one.

To check what a ViewModel saves across process death, use `testStateStore()`. See
[Surviving process death](process-death.md#testing-it).

## What to test

| Test                                                 | How                                                        |
|------------------------------------------------------|------------------------------------------------------------|
| An action changes the state                          | `dispatch`, then `assertStates` or `assertState`.          |
| An action posts an event                             | `dispatch`, then `assertEvents`.                           |
| A failure shows the right dialog / error             | Inject a mock that fails, assert the field of the state.   |
| A slow response keeps the loading state              | Inject a mock with latency, `advanceTimeBy`, assert the state. |
| The initializer is applied                           | `onCreated(initializer)`, assert the state.                |
| Every piece can run                                  | `assertSetup()`.                                           |
| The state's derived properties                       | Plain unit tests on the data class.                        |

## Testing screens

Compose screen contents are stateless: `onState(state, actions)` only draws what it receives. Draw it with any state
(see [Previews](../compose/screens.md#previews)) and verify it with Compose UI tests or screenshot tests, without a ViewModel.
