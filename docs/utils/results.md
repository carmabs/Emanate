# `EmaResult<T, E>`

Like `kotlin.Result`, but the failure can be **any type**, not only `Throwable`. It makes the failures part of the
contract of a function: instead of throwing, the function returns either a success with the data or a failure with an
error you modeled.

```kotlin
sealed interface FriendsError {
    data object NoConnection : FriendsError
    data class Server(val code: Int) : FriendsError
}

fun getFriends(query: String): EmaResult<List<User>, FriendsError>
```

You can return it from any layer, not only from [use cases](use-cases.md): a repository, a data source or a mapper can
use it too.

## Turn a result into actions

A good practice in a [side effect](../concepts/side-effects.md) is to turn each case of the `EmaResult` that a use case
returns into an action, so the reducers handle the outcome like any other change of the state. It isn't mandatory: the
result can also be consumed anywhere else.

```kotlin
getFriendsUseCase(input)
    .onSuccess { friends -> dispatch(HomeAction.SideEffects.FriendsLoaded(friends)) }
    .onFailure { error -> dispatch(HomeAction.SideEffects.FriendsLoadFailed(error)) }
```

## Functions

The examples use these two results:

```kotlin
val ok: EmaResult<Int, String> = EmaResult.success(3)
val ko: EmaResult<Int, String> = EmaResult.failure("no connection")
```

### Check and read

```kotlin
ok.isSuccess                  // true
ko.isFailure                  // true

ok.getOrNull()                // 3
ko.getOrNull()                // null

ko.getOrDefault(0)            // 0
ok.getOrDefault(0)            // 3

ko.getFailureOrNull()         // "no connection"
ok.getFailureOrNull()         // null

ok.getOrThrow()               // 3
ko.getOrThrow()               // throws IllegalArgumentException("no connection")
                              // (if the failure is a Throwable, it throws that one)
```

### Transform the success

`map` and `flatMap` only run their block when the result is a success. A failure goes through untouched.

```kotlin
ok.map { it * 2 }             // Success(6)
ko.map { it * 2 }             // Failure("no connection"), the block doesn't run

// flatMap chains an operation that can fail too
ok.flatMap { if (it > 0) EmaResult.success("$it") else EmaResult.failure("negative") }
                              // Success("3")
ko.flatMap { EmaResult.success("$it") }
                              // Failure("no connection"), the block doesn't run
```

### Transform the failure

`mapError` and `flatMapError` only run their block when the result is a failure. A success goes through untouched.

```kotlin
ko.mapError { FriendsError.NoConnection }   // Failure(NoConnection)
ok.mapError { FriendsError.NoConnection }   // Success(3), the block doesn't run

// flatMapError tries to recover with another result, for example a cache
ko.flatMapError { EmaResult.success(0) }    // Success(0)
ok.flatMapError { EmaResult.success(0) }    // Success(3), the block doesn't run
```

### Transform the whole result

```kotlin
// mapResult receives the result, success or failure, and returns another one
ok.mapResult { result -> EmaResult.success<String, String>(result.getOrDefault(-1).toString()) }
                              // Success("3")
ko.mapResult { result -> EmaResult.success<String, String>(result.getOrDefault(-1).toString()) }
                              // Success("-1")
```

### React without changing it

`onSuccess` and `onFailure` run the block only in their case and return the same result, so they chain.

```kotlin
ok.onSuccess { println("got $it") }         // prints "got 3", returns Success(3)
ko.onSuccess { println("got $it") }         // prints nothing, returns Failure("no connection")

ko.onFailure { println("failed: $it") }     // prints "failed: no connection", returns the same Failure

ok.onSuccess { println("got $it") }
  .onFailure { println("failed: $it") }     // only the first one prints
```

### Combine them

Each function returns an `EmaResult`, so they chain. For example, map the data of a repository to the model of the
screen and the error to the type your use case exposes:

```kotlin
repository.getFriends(query)                       // EmaResult<List<UserDto>, HttpError>
    .map { dtos -> dtos.map { it.toUser() } }      // EmaResult<List<User>, HttpError>
    .mapError { error -> error.toFriendsError() }  // EmaResult<List<User>, FriendsError>
```

Recover from a failure with a fallback:

```kotlin
remote.getFriends(query)
    .flatMapError { local.getFriends(query) }      // if the server fails, use the cache
```

## From `kotlin.Result`

`Result<S>.toEmaResult { throwable -> error }` converts a `Result`, parsing the exception into your failure type:

```kotlin
runCatching { api.getFriends() }
    .toEmaResult { throwable -> FriendsError.Server(code = 500) }
```
