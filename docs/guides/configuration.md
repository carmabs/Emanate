# Configuration

Emanate has a configuration for the whole application, set once, when it starts, with `Emanate.init`:

```kotlin
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Emanate.init(EmaConfiguration.Android(debug = BuildConfig.DEBUG))
    }
}
```

- Call it **before any ViewModel is created**: ViewModels read the configuration when they are built.
- It can be called **only once**. A second call throws `IllegalStateException`.
- If it is never called, the default `EmaConfiguration()` is used. That is the usual case in unit tests and in
  non-Android platforms.

Register the application in `AndroidManifest.xml` (`android:name=".MyApplication"`). A ViewModel or a source can also
receive its own configuration, which is used instead of the one of the application. See
[Explicit configuration](#explicit-configuration).

## What can be configured

Each property configures one thing. The ones that hold an object with several parameters list them in their own column, before what the property configures.

| Property             | Default `EmaConfiguration()`   | Parameters | What it configures |
|----------------------|--------------------------------|------------|--------------------|
| `dispatchers` | `EmaDispatchers()` | **`main`**: where ViewModels and sources run, and so their reducers and side effects. Default `Dispatchers.Main`.<br>**`io`**: where [use cases](use-cases.md) run, for network, disk and blocking work. Default `Dispatchers.IO` (`Dispatchers.Default` on JS and Wasm).<br>**`default`**: CPU-intensive work, like processing images. Default `Dispatchers.Default`. | The coroutine dispatchers. See [Dispatchers](#dispatchers). |
| `errorPolicy` | `EmaErrorPolicy.Catch()` | **`Catch(onError)`**: the exception is caught and the screen keeps working. `onError` is optional and receives each `EmaError`, to send it to a crash reporter.<br>**`Throw`**: the exception is thrown again and the app fails at once. | What happens when a middleware, reducer or side effect throws an exception. See [Errors](#errors). |
| `logger` | `EmaLogger.Println` | **`log(tag, message)`**: writes one line. The tag is the name of the ViewModel.<br>**`prefix`**: text added before every tag, to filter the logs of Emanate, like `EMA_HomeViewModel`. Set it with `EmaLogger(prefix) { tag, message -> ... }`. | Where the logs go. `null` disables them. See [Logs](#logs). |
| `printer` | `EmaDataClassPrinter.ToString` | **`print(value)`**: returns the text of the state or the action. By default its `toString()`.<br>**`indent`** and **`wrappedLineWidth`**: only in `EmaAndroidDataClassPrinter`. The spaces of each nesting level (2) and the length from which a long `String` is split into lines (80). | How the states and actions are turned into text in the logs. See [Printing the state](#printing-the-state). |
| `closeTimeout` | 5 seconds |  | How long the work of a closed ViewModel can still run before it is cancelled. See [Closing a ViewModel](#closing-a-viewmodel). |
| `sideEffectListener` | `null` | **`onSuccess(data, duration)`**: it finished without error. Receives its `Reflection`, the value it returned and how long it took.<br>**`onError(error, duration)`**: it threw an exception. Receives the `EmaError` and how long it took.<br>**`onFinish(reflection, duration)`**: it ended, with success, with error or cancelled. | Receives every side effect that ends, to track or measure it. See [Measuring the side effects](#measuring-the-side-effects). |
| `methodNameResolver` | `EmaMethodNameResolver.Default` | **`resolve(container, action)`**: returns the name from the ViewModel and the lambda it runs. By default `Method in ` followed by the name of the ViewModel class, like `Method in LoginViewModel`. | The name of the work that has no name of its own, in the errors and in the `sideEffectListener`. See [Naming the side effects](#naming-the-side-effects). |
| `broadcaster` | `EmaActionBroadcaster.Shared` | **`Shared`**: used by the whole process, so every ViewModel receives the actions broadcast by the others.<br>**`EmaActionBroadcaster()`**: a new one isolates the ViewModels that use it, for example the ones of a library. | Delivers the actions broadcast between ViewModels. See [Libraries and SDKs](#libraries-and-sdks). |

### Configuration for Android

`EmaConfiguration.Android(debug)` is a predefined configuration in `emanate-android`
(`com.carmabs.emanate.android.configuration.Android`). It is the one to use in an Android application, with
`BuildConfig.DEBUG` as the flag. It changes these properties from the default configuration. Each column says the value that the property has, so
you can compare them:

| Property             | `EmaConfiguration()`           | `Android(debug = true)`          | `Android(debug = false)`         |
|----------------------|--------------------------------|----------------------------------|----------------------------------|
| `errorPolicy`        | `Catch()`                      | `Throw`                          | `Catch()`                        |
| `logger`             | `EmaLogger.Println`            | `EmaLogger.Logcat`               | `null`                           |
| `dispatchers.main`   | `Dispatchers.Main`             | `Dispatchers.Main.immediate`     | `Dispatchers.Main.immediate`     |
| `printer`            | `EmaDataClassPrinter.ToString` | `EmaAndroidDataClassPrinter()`   | `EmaAndroidDataClassPrinter()`   |
| `methodNameResolver` | `EmaMethodNameResolver.Default`| `EmaAndroidMethodNameResolver`   | `EmaAndroidMethodNameResolver`   |

- **`errorPolicy`.** With `Throw`, an exception in a middleware, reducer or side effect crashes the app at once, so you
  notice it while developing. With `Catch()`, it is caught and the screen keeps working. See [Errors](#errors).
- **`logger`.** In debug builds every action and state is written to logcat with `Log.d`. In release builds nothing is
  logged, so the state of the user, which may have personal data, never reaches logcat. See [Logs](#logs).
- **`dispatchers.main`.** `Dispatchers.Main.immediate` is what the `viewModelScope` of Android uses: the work started
  from the main thread, like dispatching an action from a click, runs at once instead of waiting for the next frame.
- **`printer`.** `EmaAndroidDataClassPrinter` prints every field, indented. In release builds there is no `logger`, so
  it isn't used. See [Printing the state](#printing-the-state).
- **`methodNameResolver`.** The name is read from the lambda. See [Naming the side effects](#naming-the-side-effects).

Only `errorPolicy` and `logger` depend on the flag. `closeTimeout`, `sideEffectListener`, `broadcaster` and the other
dispatchers keep the default values in all of them.

`EmaConfiguration` is a data class, so change only what you need with `copy`:

```kotlin
Emanate.init(
    EmaConfiguration.Android(debug = BuildConfig.DEBUG).copy(
        closeTimeout = 10.seconds
    )
)
```

## Dispatchers

`EmaConfiguration.Android(debug)` uses `Dispatchers.Main.immediate` as `main`, as the `viewModelScope` of Android does: the
work started from the main thread, such as dispatching an action from a click, runs at once instead of waiting for the
next frame.

Change any of them with `copy`, keeping the others:

```kotlin
val android = EmaConfiguration.Android(debug = BuildConfig.DEBUG)
Emanate.init(android.copy(dispatchers = android.dispatchers.copy(io = myIoDispatcher)))
```

## Printing the state

The logs print each state and action with the `printer` of the configuration. Take this state:

```kotlin
data class HomeState(
    val query: String = "",
    val isLoading: Boolean = false,
    val friends: List<User> = emptyList()
) : EmaState
```

With the default printer, `EmaDataClassPrinter.ToString`, it is its `toString()`, in a single line:

```
HomeState(query=An, isLoading=false, friends=[User(name=Ana, role=USER), User(name=Andrés, role=ADMIN)])
```

`EmaConfiguration.Android(debug)` uses `EmaAndroidDataClassPrinter`, which reads the fields and indents the nested
objects, collections and maps, so a big state can be read at a glance:

```
HomeState(
  query = "An"
  isLoading = false
  friends = [
              User(
                name = "Ana"
                role = Role.USER
              ),
              User(
                name = "Andrés"
                role = Role.ADMIN
              )
            ]
)
```

It reads the fields with Java reflection, so it is only available on Android and the JVM. Change its indentation or
the width where long texts are split with `EmaAndroidDataClassPrinter(indent = 4, wrappedLineWidth = 120)`.

### A custom printer

Implement `EmaDataClassPrinter` to print the states your way. A useful case is **hiding sensitive data**, so it doesn't
reach the logs of a QA build:

```kotlin
val maskedPrinter = EmaDataClassPrinter { value ->
    value.toString()
        .replace(Regex("(password|token)=[^,)]*"), "$1=***")
}

Emanate.init(EmaConfiguration.Android(debug = BuildConfig.DEBUG).copy(printer = maskedPrinter))
```

```
LoginState(user=ana, password=***, isLoading=false)
```

To print any object with the printer of the configuration, use `toStringPretty()`.

## Logs

Emanate writes its logs with the `logger` of the configuration. A logger is a function that receives a `tag`, the
name of the ViewModel, and a `message`:

| Logger                         | Where it writes                                           | Used by                              |
|--------------------------------|-----------------------------------------------------------|--------------------------------------|
| `EmaLogger.Println`            | The standard output, with `EMA_` before the tag.          | `EmaConfiguration()`                 |
| `EmaLogger.Logcat`             | Logcat with `Log.d`, with `EMA_` before the tag.          | `EmaConfiguration.Android(true)`     |
| `null`                         | Nowhere.                                                  | `EmaConfiguration.Android(false)`    |
| Your own `EmaLogger`           | Wherever you want.                                        | Anything                             |

Every `EmaViewModel` logs its actions with its name as tag:

- One line when an action is received, before the middlewares. It shows the actions that a middleware stops, which
  don't reach the reducers.
- A block with the action and the state before and after its reducers. It is written right before them, after all
  the middlewares, so the state after the action is correct even if a middleware continues it later with `nextAfter`,
  and the action is the one the reducers receive.

For the action `QueryWritten("An")` of the state above, logcat shows (the tag, `EMA_HomeViewModel`, is omitted):

```
Received action: QueryWritten(query=An)

**********************    STARTING EMA ACTION LOGGING    ***********************
**                                                                            **
|||||||||||||||||||||||||||    Dispatched action    ||||||||||||||||||||||||||||
||                                                                            ||
||    QueryWritten(query=An)                                                  ||
|||||||||||||||||||||||    State before apply action    ||||||||||||||||||||||||
||                                                                            ||
||    HomeState(                                                              ||
||      query = ""                                                            ||
||      isLoading = false                                                     ||
||      friends = []                                                          ||
||    )                                                                       ||
||||||||||||||||||||||||    State after apply action    ||||||||||||||||||||||||
||                                                                            ||
||    HomeState(                                                              ||
||      query = "An"                                                          ||
||      isLoading = false                                                     ||
||      friends = []                                                          ||
||    )                                                                       ||
||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
**                                                                            **
**********************    FINISHED EMA ACTION LOGGING    ***********************
```

The errors caught by the [error policy](#errors) are logged too, with the same tag.

The state may contain data of the user, so don't log in release builds, as `EmaConfiguration.Android(debug)` does.

### A custom logger

An `EmaLogger` is a lambda, so it can send the logs wherever you need. To use Timber in debug builds:

```kotlin
Emanate.init(
    EmaConfiguration.Android(debug = BuildConfig.DEBUG).copy(
        logger = if (BuildConfig.DEBUG) EmaLogger { tag, message -> Timber.tag(tag).d(message) } else null
    )
)
```

Or keep the last actions as breadcrumbs of a crash report, and skip the big blocks of state:

```kotlin
val breadcrumbs = EmaLogger { tag, message ->
    // "Received action: ..." is one line and enough to know what the user did before the crash
    if (message.startsWith("Received action")) FirebaseCrashlytics.getInstance().log("$tag: $message")
}
```

Use `EmaLogger(prefix = "MYAPP_") { tag, message -> ... }` to add a prefix to every tag, so they can be filtered in
logcat.

To disable the logs of a single ViewModel, pass it a configuration without logger:
`Emanate.configuration.copy(logger = null)`. A source used on its own doesn't log: add `loggerMiddleware(tag, logger)`
as its last middleware.

## Errors

The code of your ViewModel can fail: a reducer that calls `friends.first()` on an empty list, a middleware that parses
a wrong value, a side effect that throws. What Emanate does then is the `errorPolicy`.

> **Errors you expect are not exceptions.** A request that can fail is not an error of this kind: return an
> [`EmaResult`](use-cases.md#emaresult) from the use case and let the side effect dispatch an action that shows the
> failure. The policy is for the **bugs**, the things you didn't expect.

Take a reducer with a bug:

```kotlin
reducers {
    add<HomeAction.FriendClicked> { action ->
        // Bug: throws NoSuchElementException when the friend isn't in the list
        state.copy(selected = state.friends.first { it.id == action.id })
    }
}
```

| | `Throw` | `Catch` |
|---|---------|---------|
| **What happens** | The exception is thrown again where it happened, so the app crashes with its stack trace. | The exception is caught. The app keeps running. |
| **The screen** | Closed by the crash. | Still working. The state is the one it had before the action. |
| **The log** | The crash of the app. | One line with the stack trace: `HomeViewModel: action FriendClicked failed: java.util.NoSuchElementException ...` |
| **`onError`** | Not called. | Called with an `EmaError`. |
| **Use it** | While developing, so the bug can't go unnoticed. It is the default of `EmaConfiguration.Android(true)`. | In production, so a bug in one action doesn't take the whole app down. It is the default of `EmaConfiguration()` and of release builds. |

What "keeps working" means depends on the piece that failed:

| It threw in...  | With `Catch`                                                                                  |
|-----------------|-----------------------------------------------------------------------------------------------|
| A reducer       | The action is discarded: the state doesn't change and the next actions are processed as usual. |
| A middleware    | The action is discarded and doesn't reach the reducers or the side effects.                   |
| A side effect   | That side effect ends. The rest of the side effects and the actions keep working.             |

Cancellations are never errors: a side effect cancelled by its concurrency, its timeout or the closing of the
ViewModel is not reported.

### Reporting the errors

With `Catch`, pass `onError` to send each error to your crash reporter, so a bug that is silent for the user is not
silent for you:

```kotlin
Emanate.init(
    EmaConfiguration.Android(debug = BuildConfig.DEBUG).copy(
        errorPolicy = if (BuildConfig.DEBUG) {
            EmaErrorPolicy.Throw
        } else {
            EmaErrorPolicy.Catch { error ->
                crashReporter.recordException(error.exception)
                crashReporter.setCustomKey("viewModel", error.reflection.containerClassName)
                crashReporter.setCustomKey("where", error.reflection.methodName)
            }
        }
    )
)
```

An `EmaError` has:

| Field                                     | Example                                          |
|-------------------------------------------|--------------------------------------------------|
| `exception`                               | The `NoSuchElementException` that was thrown.    |
| `reflection.containerClassName`           | `HomeViewModel`                                  |
| `reflection.containerClassQualifiedName`  | `com.carmabs.sample.home.HomeViewModel`          |
| `reflection.methodName`                   | `action FriendClicked` for a middleware or a reducer. For a side effect, its name: `RefreshRequested`, see [Naming the side effects](#naming-the-side-effects). |

To crash also in the QA builds, so the testers report the bugs instead of the app hiding them, use `Throw` there: the
`debug` flag is only the default of `EmaConfiguration.Android`, and the policy is yours to choose.

## Measuring the side effects

To track the side effects, for example to measure how long they take, count how many fail or send them to your
analytics, set a `sideEffectListener`. It is called for every side effect of every ViewModel, and it has three
functions, all optional. Each one receives the `duration` the side effect took:

| Function                          | Called when                                                  | Receives                                       |
|-----------------------------------|--------------------------------------------------------------|------------------------------------------------|
| `onSuccess(data, duration)`       | A side effect finishes without error.                        | A `ReflectionData`: the `reflection` and the value `data` that the side effect returned. |
| `onError(error, duration)`        | A side effect throws, with `Catch` and with `Throw` (right before the app fails). | An `EmaError`: the `reflection` and the `exception`. |
| `onFinish(reflection, duration)`  | A side effect ends: after `onSuccess`, after `onError`, or when it is cancelled. | The `reflection`.              |

A cancellation (a new action with `Latest`, a timeout, the closing of the ViewModel) is not an error, so it only calls
`onFinish`. That is how you tell the three endings apart: `onSuccess` is a success, `onError` a failure, and an
`onFinish` with neither before it is a cancellation.

The `Reflection` says which side effect it is:

| Field                         | Example                                  |
|-------------------------------|------------------------------------------|
| `containerClassName`          | `HomeViewModel`                          |
| `containerClassQualifiedName` | `com.carmabs.sample.home.HomeViewModel`  |
| `methodName`                  | The name of the side effect, see [Naming the side effects](#naming-the-side-effects). |

With them you can track, for each side effect, which ViewModel launched it, its name, how long it took and how it
ended:

```kotlin
class SideEffectTracker(private val analytics: Analytics) : EmaSideEffectListener {

    override fun onSuccess(data: ReflectionData, duration: Duration) {
        analytics.timing(
            name = "${data.reflection.containerClassName}.${data.reflection.methodName}",
            millis = duration.inWholeMilliseconds
        )
    }

    override fun onError(error: EmaError, duration: Duration) {
        analytics.track(
            event = "side_effect_error",
            "viewModel" to error.reflection.containerClassName,
            "name" to error.reflection.methodName,
            "error" to error.exception::class.simpleName.toString(),
            "millis" to duration.inWholeMilliseconds.toString()
        )
    }

    override fun onFinish(reflection: Reflection, duration: Duration) {
        // Also called by the cancelled ones
    }
}

Emanate.init(
    EmaConfiguration.Android(debug = BuildConfig.DEBUG).copy(sideEffectListener = SideEffectTracker(analytics))
)
```

- **`onSuccess` and `onError` are for tracking, not for handling.** The error is also handled by the
  [error policy](#errors): `onError` of the listener tells you about **that side effect and its time**, while the
  `onError` of `EmaErrorPolicy.Catch` is the place to report bugs.
- **The `data` of the side effects of a `setup` is always `Unit`**, because the lambda that Emanate runs doesn't
  return what yours does. Use the `reflection` and the `duration`.
- **The duration** is the time the side effect was running, including the `delay`s and the suspensions. A side effect
  that ends by its `timeout` calls `onSuccess` after the time of the timeout.

## Naming the side effects

The `methodName` identifies the side effect in the errors and in the `sideEffectListener`. A side effect declared in
the `setup` of a ViewModel is named after **the type of the actions that launch it**:

```kotlin
sideEffects {
    add<HomeAction.RefreshRequested> { /* ... */ }                       // RefreshRequested
    add<HomeAction.FriendClicked>(name = "openProfile") { /* ... */ }    // openProfile
}
```

Pass `name` to `add`, to `sideEffectOf` or to the `name` property of a class that implements `SideEffect`, when the
type of the action isn't a good name, for example when two side effects listen to the same action or to a base type
like `EmaAction`. The `onClose` blocks are named `onClose`.

The other work that Emanate launches takes its name from the `methodNameResolver`, which receives the object that
launched it, usually the ViewModel, and the lambda that it runs:

| Resolver                       | Result                                                                  |
|--------------------------------|-------------------------------------------------------------------------|
| `EmaMethodNameResolver.Default` | A generic name: `Method in ` followed by the name of the ViewModel class, like `Method in LoginViewModel`.                              |
| `EmaAndroidMethodNameResolver` | Reads the name from the class that the compiler generates for the lambda, so a lambda written in `doLogin` of `LoginViewModel` (`LoginViewModel$doLogin$1`) is named `doLogin`. If it can't find it, it uses the generic one. |
| Your own                       | Anything: `EmaMethodNameResolver { container, action -> "${container::class.simpleName}.work" }`. |

## Closing a ViewModel

When a ViewModel is closed, its side effects are cancelled, so the work that was running stops. Two things can still
run after it:

- the `onClose` blocks of its source, which are the last thing the ViewModel does, and
- the side effects added with `survivesClose = true`, like the one that saves a draft when the user leaves the editor.

The `closeTimeout` is how long they have. After it, they are cancelled, so a request that never answers doesn't keep
the ViewModel alive for ever. By default it is 5 seconds.

```kotlin
sideEffects {
    add<EditorAction.SaveClicked>(survivesClose = true) { saveDraftUseCase(state.draft) }
}
```

If the user leaves the editor and the save takes 8 seconds, it is cancelled at the 5th. For slower work, increase it:

```kotlin
Emanate.init(EmaConfiguration.Android(debug = BuildConfig.DEBUG).copy(closeTimeout = 15.seconds))
```

Keep it short: it is the time a ViewModel that was already closed stays in memory. See
[Side effects](../concepts/side-effects.md#when-the-screen-is-closed).

## Explicit configuration

`Emanate.configuration` is shared by the whole process. `EmaViewModel` and `EmaSource(...)` read it when they are created, as the default value of their `configuration` parameter, which goes
right after the initial state. Pass one to use it instead:

```kotlin
class HomeViewModel(
    private val getFriendsUseCase: GetFriendsUseCase,
    configuration: EmaConfiguration = Emanate.configuration
) : EmaViewModel<HomeState, HomeAction, HomeEvent>(HomeState(), configuration) {
    // ...
}
```

The ViewModel runs on its `main` dispatcher and logs with its `logger`. The configuration is kept by the ViewModel and its source, so its error policy, side effect listener,
`closeTimeout` and broadcaster are the ones of the configuration they received.

**Use `Emanate.init`** in an application: it configures every ViewModel in one place, and they don't need any
parameter. **Pass the configuration** when the one of the application isn't enough or can't be relied on:

- **Libraries and SDKs.** `Emanate.init` can be called only once and belongs to the application. A library built with
  Emanate passes its own configuration to its ViewModels, so it doesn't clash with an application that also uses
  Emanate, nor depend on whether it was initialized. See [Libraries and SDKs](#libraries-and-sdks).
- **A module with a different configuration**, for example its own logger or error policy.
- **Dependency injection.** The configuration is one more dependency, provided by Hilt or Koin. See
  [Dependency injection](dependency-injection.md#injecting-the-configuration).
- **Tests.** Each test passes its configuration, with the dispatchers of the test, without changing the one of Emanate,
  so the tests can run in parallel. See [Testing](testing.md#without-changing-the-configuration-of-emanate).

The [use cases](use-cases.md) don't receive the whole configuration, only a dispatcher in the constructor:
by default the `io` dispatcher of the configuration of Emanate, so pass the `io` dispatcher of yours.

## Libraries and SDKs

A library built with Emanate can be used by an application that also uses Emanate and calls `Emanate.init`. To keep
them apart, the library doesn't call `Emanate.init` nor use the configuration of Emanate: it creates its own and
passes it to all its pieces.

```kotlin
internal object MyLibraryEmanate {
    val configuration = EmaConfiguration.Android(debug = BuildConfig.DEBUG).copy(
        logger = if (BuildConfig.DEBUG) EmaLogger(prefix = "MYLIB_") { tag, message -> Log.d(tag, message) } else null,
        broadcaster = EmaActionBroadcaster()
    )
}
```

### Wrap the classes of Emanate in base classes

Passing the configuration in every ViewModel and use case of the library is easy to forget: one that doesn't receive it
silently uses the one of the application, or the default one if the application never initialized Emanate. **Don't
extend the classes of Emanate directly in the library: create a base class for each one that already passes the
configuration**, and extend only those:

```kotlin
internal abstract class MyLibViewModel<S : EmaState, A : EmaAction, E : EmaEvent>(initialState: S) :
    EmaViewModel<S, A, E>(initialState, MyLibraryEmanate.configuration)

internal abstract class MyLibUseCase<I, O> :
    EmaUseCase<I, O>(MyLibraryEmanate.configuration.dispatchers.io)

internal abstract class MyLibFlowUseCase<I, O> :
    EmaFlowUseCase<I, O>(MyLibraryEmanate.configuration.dispatchers.io)
```

The classes of the library extend them, and there is nothing to remember:

```kotlin
internal class PayUseCase(private val repository: PaymentRepository) : MyLibUseCase<Payment, Receipt>() {
    // ...
}

internal class PaymentViewModel(private val payUseCase: PayUseCase) :
    MyLibViewModel<PaymentState, PaymentAction, PaymentEvent>(PaymentState()) {
    // ...
}
```

The base classes are also the place to add what every piece of the library shares, like a plugin that all its
ViewModels install. If the library creates a source on its own with `EmaSource(...)`, wrap it the same way.

- **Broadcast actions.** With its own `EmaActionBroadcaster`, the ViewModels of the library only receive the actions
  broadcast by each other, and the ones of the application don't receive theirs. Keep the shared one,
  `EmaActionBroadcaster.Shared`, if the library and the application send results to each other.
- **Public ViewModels.** If the application must create the ViewModels of the library, expose factories or a public
  base class of your own, not the classes of Emanate, so the configuration can't be skipped.

If the application must be able to change something, like the logger, add a function to the library that receives it
before any of its screens is opened, instead of using `Emanate.init`.

The application and the library use the same `emanate-core`: the version that Gradle resolves for both. Keep the library
on a version compatible with the applications that use it.
