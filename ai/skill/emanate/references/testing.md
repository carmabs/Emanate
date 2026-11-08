# Testing with emanate-test

Add the `emanate-test` artifact with `testImplementation` in the module with the ViewModels (JVM). A ViewModel is plain Kotlin: no emulator.

```kotlin
class HomeViewModelTest {
    @BeforeTest fun setUp() { useTestConfiguration() }        // dispatchers + Main on the test dispatcher
    @AfterTest fun tearDown() { resetTestConfiguration() }    // otherwise tests leak into each other

    @Test fun `a search shows the matching friends`() = runTest {
        val viewModel = HomeViewModel(GetFriendsUseCase(FakeRepository()), HomeState.DEFAULT)  // AFTER useTestConfiguration
        val recorder = viewModel.record()                                                 // BEFORE the first action
        viewModel.dispatch(HomeAction.QueryWritten("An"))
        viewModel.dispatch(HomeAction.RefreshRequested)
        advanceUntilIdle()                                                                // runs side effects and delays
        recorder.assertState { !it.isLoading && it.friends == listOf(ana) }
        recorder.assertEvents(HomeEvent.FriendsUpdated(1))
    }

    @Test fun `every piece can run`() { HomeViewModel(...).assertSetup() }  // catches missing listen<Type>()
}
```

| Utility | Use |
|---|---|
| `record()` -> `EmaRecorder` | `.actions`, `.states` (reducer outputs, no initial state), `.events` |
| `assertActions(...)`, `assertStates(...)`, `assertState(s)` / `assertState { }`, `assertEvents(...)`, `assertNoEvents()` | Whole list, in order |
| `assertSetup()` | Fails if a piece handles an action type the ViewModel never receives |
| `onCreated(initializer)` | Test the initializer |
| `testConfiguration(dispatcher)` | Explicit configuration passed to the ViewModel (no shared global, parallel-safe, own broadcaster); build the dispatcher with `testScheduler`; ViewModels that talk through broadcasts must share the same configuration |
| `useTestConfiguration(StandardTestDispatcher())` | Work runs only when the test advances (check what happens between action and result) |
| `testStateStore()` / `restoreStateOf(...)` | In-memory store for backups |

What to test: action -> state (`assertStates`), action -> event, failure -> dialog (inject a failing fake), slow response keeps loading (`advanceTimeBy`), initializer applied, derived state properties as plain unit tests.
Use cases run on the `io` dispatcher unless given another in the constructor: pass the test dispatcher or fake them.
