# Testing

An `EmaViewModel` has no platform dependencies, so you test it as plain Kotlin, with no device and no emulator. And because
every change goes through its `EmaSource`, a test can see **everything** the screen did, in order: each action, each state
and each event.

| You control                                        | You check                                         |
|----------------------------------------------------|---------------------------------------------------|
| The actions, with `dispatch` and `onCreated`       | The states returned by the reducers, in order     |
| The pieces it receives: fakes, mocks, delays       | The events posted, in order                       |
| The time, with the virtual clock of `runTest`      | The actions, including the ones a middleware stopped |

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

## Testing an EmaViewModel with emanate-test

`emanate-test` has the utilities to test `EmaViewModel` and
[`EmaSource`](../concepts/source.md). Add it to the unit tests of the module of your ViewModels (for now it is published for
the JVM test source sets):

```kotlin
dependencies {
    testImplementation("com.github.carmabs.ema:emanate-test:$emanateVersion")
}
```

It brings `kotlinx-coroutines-test` and requires the same version of `emanate-core` it was built with.

These are all the utilities of `emanate-test`. Each one is explained below, with an example, why it matters and what
happens if you don't use it.

| Utility                         | In one line                                                                   |
|---------------------------------|-------------------------------------------------------------------------------|
| [`useTestConfiguration()`](#install-the-test-configuration) | Installs in Emanate a configuration that runs the ViewModels, `EmaSource` and use cases on the dispatcher of the test. |
| [`resetTestConfiguration()`](#install-the-test-configuration) | Undoes the previous one, so a test doesn't affect the next. |
| [`testConfiguration(dispatcher)`](#without-changing-the-configuration-of-emanate) | The same, but passed to the ViewModel instead of changing what is shared. |
| [`viewModel.record()`](#recording-a-viewmodel) | Starts recording the actions, states and events. |
| [`assertActions(...)`](#checking-what-was-recorded) | The recorded actions, including the ones blocked by a middleware. |
| [`assertStates(...)`](#checking-what-was-recorded) | Every state returned by the reducers, in order. |
| [`assertState(state)` / `assertState { }`](#checking-what-was-recorded) | The last state, or a condition on it. |
| [`assertEvents(...)` / `assertNoEvents()`](#checking-what-was-recorded) | The posted events. |
| [`viewModel.assertSetup()`](#checking-the-setup) | Fails if a middleware, reducer or side effect never runs. |
| [`testStateStore()` / `restoreStateOf(...)`](#process-death-testing) | A store in memory to test what a ViewModel saves and restores. |

## Install the test configuration

`useTestConfiguration()` and `resetTestConfiguration()` go together: one before each test, the other after it.

**What `useTestConfiguration()` does.** It changes `Emanate.configuration`, shared by the whole process, so the
ViewModels, `EmaSource` and use cases created after it run on the dispatcher of the test, a `TestDispatcher` from
`kotlinx-coroutines-test`. It keeps the rest of the current configuration (logger, error policy...) and only replaces the
dispatchers. It also replaces `Dispatchers.Main` with it (`Dispatchers.setMain`), because in the unit tests there is no
real `Main`, and so `runTest` uses the same scheduler. By default it is an `UnconfinedTestDispatcher`, which processes
each action as soon as it is dispatched. It returns the dispatcher.

**Why it matters.** The work of a ViewModel (reducers, side effects, delays, use cases) is asynchronous. On the test
dispatcher it runs under the virtual clock of `runTest`: `advanceUntilIdle()` finishes it, and a `delay(3.seconds)` takes
no time. The test is deterministic and fast.

**What happens without it.**

- A ViewModel uses `Dispatchers.Main`, which doesn't exist in the unit tests of the JVM, so it fails with
  `Module with the Main dispatcher had failed to initialize`.
- Even if you replace `Main` yourself, the side effects and use cases would run on the real `io` dispatcher, in other
  threads. `runTest` doesn't wait for them, so the assertions run before the work ends and the test passes or fails
  at random. A real `delay` makes it as slow as the delay.

**What `resetTestConfiguration()` does.** It restores `Dispatchers.Main` and the configuration of Emanate, and removes the
actions broadcast with `keepUntilReceived` that nobody received.

**What happens without it.** The next tests start with the `Main` and the configuration of this one, and with its
pending broadcast actions: a test can receive an action that another test sent. The failures depend on the order of the
tests, which is the hardest kind to find.

**How to use it.**

```kotlin
class HomeViewModelTest {

    @BeforeTest
    fun setUp() {
        useTestConfiguration()
    }

    @AfterTest
    fun tearDown() {
        resetTestConfiguration()
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

- **Create the ViewModel after `useTestConfiguration()`**, because it reads the configuration when it is created.
- **`advanceUntilIdle()`** runs the side effects, delays included, because `runTest` uses the scheduler of the test
  dispatcher.

### Advanced: choosing when the work runs

You don't need this to write your first tests. `useTestConfiguration()` and `testConfiguration()` accept any
`TestDispatcher`, and the two you can choose differ in **when the work that is launched runs**:

| | `UnconfinedTestDispatcher` (default) | `StandardTestDispatcher` |
|---|---|---|
| When the work runs | As soon as it is launched | When the test advances: `advanceUntilIdle()`, `advanceTimeBy()` or `runCurrent()` |
| After `dispatch(action)` | The reducer has already run, and the side effect too if it doesn't wait | Nothing has run: the action is in a queue |
| Two `dispatch` in a row | The first one is finished before the second one starts | Both are queued and processed together when the test advances |
| Use it for | Most tests: dispatch, advance and check the result | Checking what happens **between** the action and its result, or the order of several actions |

With `StandardTestDispatcher` the test controls the moment, so you can check that something has **not** happened yet:

```kotlin
@BeforeTest
fun setUp() {
    useTestConfiguration(StandardTestDispatcher())
}

@Test
fun `the refresh is processed when the test advances`() = runTest {
    val viewModel = HomeViewModel(GetFriendsUseCase(FakeRepository()), HomeState())
    val recorder = viewModel.record()

    viewModel.dispatch(HomeAction.RefreshRequested)
    recorder.assertActions()                                   // still waiting in the queue

    advanceUntilIdle()
    recorder.assertActions(HomeAction.RefreshRequested, HomeAction.EffectQueryResult.Loaded(listOf(ana)))
}
```

- **Why not always `StandardTestDispatcher`?** Because every test would need to advance after each action, even the
  ones that don't care about the order. `UnconfinedTestDispatcher` is the default to keep the common test short.
- **Why use it?** It is closer to production, where `Dispatchers.Main` doesn't run the work at once but queues it. A
  test with two actions in a row can fail with it and pass with the unconfined one, because the unconfined one hides
  an order problem.
- With `testConfiguration`, create it with the scheduler of the test:
  `testConfiguration(StandardTestDispatcher(testScheduler))`.

### Without changing the configuration of Emanate

`useTestConfiguration()` changes the configuration of Emanate and `Dispatchers.Main`, which are shared by the whole
process, so the tests that use it can't run in parallel in the same process. If your ViewModels receive the configuration in
the constructor (see [Explicit configuration](configuration.md#explicit-configuration)), pass them
`testConfiguration(dispatcher)` instead: it doesn't change anything shared, so there is nothing to reset.

**What it does.** It returns an `EmaConfiguration` whose `main`, `io` and `default` dispatchers are the one you give it.
The rest is the default `EmaConfiguration()`, not the one of Emanate, so the test doesn't depend on `Emanate.init`. It
also gives the configuration its own broadcaster.

**Why it matters.** Nothing shared changes, so the tests can run in parallel and there is no `@AfterTest`. Each test
owns its dispatcher and its broadcaster.

**What happens without it.** If you use neither this nor `useTestConfiguration()`, the ViewModel gets
`Emanate.configuration` and runs with the real dispatchers, with the problems explained above. If you share the broadcaster
of Emanate between tests that run in parallel, the actions broadcast in one reach the ViewModels of the others.

**How to use it.**

```kotlin
class GetUnreadCountUseCase(
    private val repository: MessagesRepository,
    dispatcher: CoroutineDispatcher = Emanate.configuration.dispatchers.io
) : EmaUseCase<Unit, Int>(dispatcher) {

    override suspend fun useCaseFunction(input: Unit) = repository.getUnread().size
}

class InboxViewModel(
    private val getUnreadCountUseCase: GetUnreadCountUseCase,
    initialState: InboxState,
    configuration: EmaConfiguration = Emanate.configuration
) : EmaViewModel<InboxState, InboxAction, InboxEvent>(initialState, configuration) {
    // ...
}

class InboxViewModelTest {

    @Test
    fun `refreshing shows the unread count`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val viewModel = InboxViewModel(
            GetUnreadCountUseCase(FakeMessagesRepository(), dispatcher),
            InboxState(),
            testConfiguration(dispatcher)
        )
        val recorder = viewModel.record()
        // ...
    }
}
```

- **Create the dispatcher with `testScheduler`**, the scheduler of `runTest`, so `advanceUntilIdle()` runs the work of
  the ViewModel and the use cases. `Dispatchers.Main` isn't replaced, so `runTest` doesn't take it from there.
- Pass another configuration as the second parameter to change the rest: `testConfiguration(dispatcher, EmaConfiguration(logger = null))`.
- The [use cases](../utils/use-cases.md) run on the `io` dispatcher of the configuration of Emanate unless they receive
  another one in the constructor, as `GetUnreadCountUseCase` does: pass them the same dispatcher as the configuration,
  or fake them. Otherwise they run on the real `io` dispatcher, out of the control of the test.
- **Each `testConfiguration` has its own broadcaster**, so the actions broadcast in a test don't reach the others. The
  ViewModels of a test that send results to each other must receive the same configuration: create it once.

## Recording a ViewModel

`viewModel.record()` (and `emaSource.record()` for an [`EmaSource`](../concepts/source.md)) starts recording and returns
an `EmaRecorder`.

**What it does.** From that moment, it keeps in order every action that enters the `EmaSource`, every state returned by a
reducer and every event posted. The recorder exposes them as lists: `recorder.actions`, `recorder.states` and
`recorder.events`.

**Why it matters.** The ViewModel only exposes its **current** state. A test that reads it can't see that the screen
went through `isLoading = true` before showing the friends, that an event was posted, or that a middleware blocked an
action. The recorder is what lets you check the behavior, not only the final result.

**What happens without it.** You can only read the last state with `viewModel.stateFlow.value`. A bug that shows a
spinner that never disappears at the right moment, a duplicated event or a missing one, goes unnoticed.

**How to use it.**

```kotlin
val viewModel = HomeViewModel(GetFriendsUseCase(FakeRepository()), HomeState())
val recorder = viewModel.record()           // before the first action

viewModel.dispatch(HomeAction.RefreshRequested)
advanceUntilIdle()

println(recorder.actions)   // [RefreshRequested, Loaded([Ana])]
println(recorder.states)    // [HomeState(isLoading=true), HomeState(friends=[Ana])]
println(recorder.events)    // [FriendsUpdated(1)]
```

- **Record before the first action.** What happened before `record()` is not recorded.
- **`states` doesn't include the initial state**, only the ones returned by the reducers.

## Checking what was recorded

The recorder compares **the whole list, in order**. If there is an extra, a missing or a misplaced element, the test
fails and the message shows both lists. You don't write the loops and the comparisons.

| Function | Checks | Example |
|----------|--------|---------|
| `assertActions(...)` | The actions that entered, in order, including the ones dispatched by side effects and the ones a middleware blocked. | `recorder.assertActions(HomeAction.RefreshRequested, HomeAction.EffectQueryResult.Loaded(listOf(ana)))` |
| `assertStates(...)` | The states returned by the reducers, one for each reduced action. | `recorder.assertStates(HomeState(isLoading = true), HomeState(friends = listOf(ana)))` |
| `assertState(state)` | The last state is exactly that one. | `recorder.assertState(HomeState(friends = listOf(ana)))` |
| `assertState { }` | The last state fulfils a condition, without writing the whole state. | `recorder.assertState { !it.isLoading && it.friends.size == 1 }` |
| `assertEvents(...)` | The posted events, in order. | `recorder.assertEvents(HomeEvent.FriendsUpdated(1))` |
| `assertNoEvents()` | No event was posted. | `recorder.assertNoEvents()` |

**Why it matters.**

- `assertActions` is the only way to know that a middleware **blocked** an action, because a blocked action doesn't
  change the state.
- `assertStates` checks the **path**, not only the destination: loading first, the result later.
- `assertState { }` keeps the test short and robust, because it doesn't break when an unrelated field of the state
  changes.
- `assertNoEvents()` is the check that something **didn't** happen: a failed load must not post
  the success event.

**What happens without them.** You compare `recorder.states` and `recorder.events` by hand. It works, but every test
repeats the same code, and a test that forgets to check the events passes when a wrong one is posted. A check also
fails with a clear message:

```
Expected actions <[Loaded(name=Ema)]> but were <[Load]>
The last state <ProfileState(name=, loading=true)> doesn't fulfil the condition
No state has been recorded
```

The last one means that no reducer ran: the action was never dispatched, was blocked, or the test didn't advance with `advanceUntilIdle()`.

The ViewModel can receive actions from outside too: the lifecycle, the initializer and the results of other screens are
actions, so a test sends them the same way as the screen does.

```kotlin
viewModel.onCreated(HomeInitializer.HomeUser(ana))   // EmaInitializer
viewModel.onResumeView()                             // EmaLifecycle.Resumed
```

## Checking the setup

`viewModel.assertSetup()` checks that every middleware, reducer and side effect of the ViewModel can receive actions of
its type: actions of the screen, initializers, lifecycle actions, the actions of its plugins or broadcast actions it
listens to.

**Why it matters.** A reducer, middleware or side effect for an action that the ViewModel never receives doesn't fail:
it never runs. It happens when a broadcast action is handled but `listen<Type>()` is missing in the `broadcasts` block.

**What happens without it.** Nothing fails, at compile time or at runtime. The user creates a friend in another screen
and the list never updates, and no test notices, because the tests of the ViewModel dispatch their actions directly.

**How to use it.** Call it on the ViewModel, in a test of its own. It needs no actions, no recorder and no
`advanceUntilIdle()`: it only reads how the ViewModel is declared.

```kotlin
@Test
fun `the setup is complete`() {
    HomeViewModel(GetFriendsUseCase(FakeRepository()), HomeState()).assertSetup()
}
```

If every piece can receive its action, the test passes. To see what it catches, take a `HomeViewModel` that handles the
`FriendCreated` broadcast of the profile screen (see [Broadcasts](../concepts/broadcasts.md)) but forgot to listen to it:

```kotlin
class HomeViewModel(/* ... */) : EmaViewModel<HomeState, HomeAction, HomeEvent>(/* ... */) {

    override fun setup() = source {
        // broadcasts { listen<ProfileBroadcast.FriendCreated>() }   <- missing

        reducers {
            add<ProfileBroadcast.FriendCreated> { action -> state.copy(friends = state.friends + action.friend) }
        }
    }
}
```

The reducer is declared, but `HomeViewModel` never receives `FriendCreated`, so it never runs and the home list never
shows the new friend. Nothing fails when the app runs, but `assertSetup()` does, and it says which piece and how to fix it:

```
The reducer of FriendCreated never runs: HomeViewModel doesn't receive FriendCreated. If it is broadcast by another
source, add listen<FriendCreated>() in the broadcasts block.
```

Adding `listen<ProfileBroadcast.FriendCreated>()` to the `broadcasts` block makes the test pass.

Only the types that the ViewModel can't receive for sure are reported. An interface can be implemented by an action of
the screen, so a piece for an interface that isn't related to the actions of the screen is not reported.

## Process death testing

`testStateStore()` and `restoreStateOf(...)` are for the ViewModels with a [backup](process-death.md) of their state.

### `testStateStore()`

**What it does.** It creates an `EmaStateStore` in memory. It lives longer than the ViewModels that use it, like the
store of the platform survives the death of the process. `store.values` shows what has been saved, by key.

**Why it matters.** To test that the form survives, you pass the same store to two ViewModels: the first one saves, the
second one, created after it, restores. That is the death of the process, without a device.

**What happens without it.** You would have to write your own `EmaStateStore`, or test it on Android with a real
`SavedStateHandle` and an instrumented test. A ViewModel that saves the wrong key, or never restores, goes
unnoticed until a user loses what they typed.

### `restoreStateOf(...)`

**What it does.** It creates the `EmaRestoreState` action with the saved texts, by key, to compare it with
`assertActions`. The values are the texts as they are in the store.

**Why it matters.** The restore is an action that goes through the middlewares and reducers. With it you check that it
arrives, and when: right after the initializer, which is applied first.

**What happens without it.** You would build the `EmaRestoreState` by hand, or you would only check the final state,
and the test couldn't tell whether the restore ran or the initializer already left that value.

### Using them together

The two work as a pair: `testStateStore()` simulates the death of the process and `restoreStateOf(...)` checks what the
new ViewModel receives after it.

```kotlin
@Test
fun `the form survives the death of the process`() = runTest {
    // 1. The first ViewModel saves in the store
    val store = testStateStore()
    val first = ProfileCreationViewModel(ProfileCreationState.DEFAULT, store)
    first.onCreated(ProfileCreationInitializer.Admin)
    first.dispatch(ProfileCreationAction.UserNameWritten("Ana"))

    assertEquals("""{"name":"Ana","surname":""}""", store.values["form"])    // what was saved

    // 2. The process dies: a new ViewModel receives the same store
    val restored = ProfileCreationViewModel(ProfileCreationState.DEFAULT, store)
    val recorder = restored.record()
    restored.onCreated(ProfileCreationInitializer.Admin)

    // 3. The restore arrives right after the initializer, with the saved values
    recorder.assertActions(
        ProfileCreationInitializer.Admin,
        restoreStateOf("form" to """{"name":"Ana","surname":""}""")
    )
    assertEquals("Ana", restored.stateFlow.value.name)
}
```

## Swapping pieces in the tests

A ViewModel that [receives its pieces](dependency-injection.md#injecting-pieces) in the constructor, like the side effect
that loads the friends, is tested with the version of that piece the test needs: one that answers at once, one that
fails, one that takes a while.

- **No mocking library is needed.** The mock is a normal class that implements the same piece, like `MockFriends` in
  [Dependency injection](dependency-injection.md#injecting-pieces). You don't configure it with `every { ... } returns`
  or `when(...).thenReturn`: you create it with the data you want, `MockFriends(friends = null)`.
- **Only the piece you swap is fake.** The reducers, the middlewares and the events of the ViewModel are the real ones,
  the same as in production. The mock dispatches the same actions as the real piece, so the test checks that the
  screen reacts correctly to them.

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

Many screens wait on purpose: a search that waits 300 ms after the last key (a debounce), a request that gives up after
10 seconds (a timeout), a loading state that lasts as long as the request. In a test you don't want to wait for any of
that.

**What it means.** `runTest` has a **virtual clock**: `delay(3.seconds)` doesn't make the test wait three seconds. The
test dispatcher records that the work has to resume 3 seconds from now, and the clock only moves when the test moves
it. So a test that covers a three-second request takes milliseconds.

**What makes it work.** The side effects and middlewares run on the dispatcher of the test (see
[Install the test configuration](#install-the-test-configuration)), so their `delay`s use the clock of `runTest`. If
they ran on a real dispatcher, each `delay` would be real.

**How you move the clock:**

| Function | What it does |
|----------|--------------|
| `advanceTimeBy(duration)` | Moves the clock forward and runs what was due **before** that moment. Work due exactly at that moment doesn't run yet. |
| `runCurrent()` | Runs what is due at the current moment, without moving the clock. |
| `advanceUntilIdle()` | Moves the clock as far as needed until there is no work left. It is the one to use when you only want the final result. |

**Checking a debounce.** Take a side effect for `QueryWritten` with `Latest` concurrency that waits 300 ms before
searching. Each new key cancels the previous wait and starts it again, so only the last query is searched:

```kotlin
viewModel.dispatch(HomeAction.QueryWritten("an"))
advanceTimeBy(200.milliseconds)
recorder.assertNoEvents()          // 200 ms: still waiting, nothing searched

viewModel.dispatch(HomeAction.QueryWritten("ana"))   // cancels the wait of "an" and starts a new one
advanceTimeBy(200.milliseconds)
recorder.assertNoEvents()          // 400 ms since "an", but only 200 ms since "ana": still waiting

advanceUntilIdle()                 // the 300 ms of "ana" pass
recorder.assertState { it.friends == listOf(ana) && it.query == "ana" }
```

Without the virtual clock this test would need real `Thread.sleep`s, would last a second and would fail on a slow machine.

**Checking a loading state.** The same goes for a mock with latency: `advanceTimeBy` stops the request halfway, so you
can check the loading state of a slow request:

```kotlin
@Test
fun `a slow load keeps the loading state until it finishes`() = runTest {
    val viewModel = HomeViewModel(loadFriends = MockFriends(friends = listOf(ana), latency = 3.seconds))
    val recorder = viewModel.record()

    viewModel.dispatch(HomeAction.RefreshRequested)
    advanceTimeBy(2.seconds)                               // 2 s of 3 s: the request is still running
    recorder.assertState { it.isLoading }

    advanceUntilIdle()                                     // the last second passes
    recorder.assertState { it.friends == listOf(ana) }
}
```

**Checking a timeout.** With `timeout = 10.seconds` and a mock that never answers, `advanceTimeBy(11.seconds)` is
enough to check that `onTimeout` ran and the state shows the error, without waiting ten seconds.

### Testing a broadcast between ViewModels

A [broadcast](../concepts/broadcasts.md) goes from one ViewModel to others, so to test it you create **both** and check
that what one sends reaches the other. For example, the profile screen creates a friend and the home list must show it.

The key is the configuration: the broadcast travels through the `broadcaster` of the configuration, so the ViewModels
that talk to each other must receive **the same** one. With `testConfiguration` create it once and pass it to both:

```kotlin
@Test
fun `a friend created in the profile reaches the home list`() = runTest {
    val configuration = testConfiguration(UnconfinedTestDispatcher(testScheduler))
    val home = HomeViewModel(GetFriendsUseCase(FakeRepository()), HomeState(), configuration)   // listens to FriendCreated
    val profile = ProfileViewModel(ProfileState(friend = ana), configuration)                    // sends FriendCreated
    val homeRecorder = home.record()

    profile.dispatch(ProfileAction.SaveClicked)       // its side effect broadcasts FriendCreated(ana)
    advanceUntilIdle()

    homeRecorder.assertActions(ProfileBroadcast.FriendCreated(ana))
    homeRecorder.assertState { it.friends == listOf(ana) }
}
```

- **Create the receiver before sending.** A broadcast is delivered to the ViewModels that listen at that moment, unless
  the sender uses `keepUntilReceived`.
- **Record the receiver**, not the sender: the test checks what arrives and how the receiver reacts.
- **If the two ViewModels got different configurations,** each one would have its own broadcaster and the action would
  never arrive. The test would fail with an empty list of actions, not with an error that says why.
- With `useTestConfiguration()` the ViewModels already share the configuration of Emanate, so you don't need to pass it.
- To check that the receiver declared `listen<FriendCreated>()` without creating the sender, use
  [`assertSetup()`](#checking-the-setup).
