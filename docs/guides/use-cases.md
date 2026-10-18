# Use cases and results

Emanate has a few small types that belong to the architecture itself: the use cases that the side effects call and the
results they return. For the texts the state holds, see [Texts without Context](texts.md).

## Use cases

A use case is one operation of the business logic, with a single `Input`. The side effects of the ViewModels call them,
and they call the repositories.

| Class                          | Use it for                                                                       |
|--------------------------------|----------------------------------------------------------------------------------|
| `EmaUseCase<I, O>`             | A `suspend` operation. Runs on the background dispatcher it receives in the constructor, by default the `io` one of the [configuration](configuration.md#dispatchers). |
| `EmaFlowUseCase<I, O>`         | An operation that emits values over time (`Flow<O>`), collected on the background dispatcher it receives in the constructor. |
| `EmaSyncUseCase<I, O>`         | A fast, synchronous operation.                                                   |

Implement `useCaseFunction` (the protected function) and call the use case like a function:

```kotlin
class GetFriendsUseCase(private val repository: FriendsRepository) :
    EmaUseCase<GetFriendsUseCase.Input, EmaResult<List<User>, FriendsError>>() {

    override suspend fun useCaseFunction(input: Input) = repository.getFriends(input.query)

    data class Input(val query: String)
}

// In a side effect
getFriendsUseCase(GetFriendsUseCase.Input(state.query))
```

The side effects run on the main dispatcher, and the use cases switch to the background one themselves, so you don't
need to. To run a use case once on another dispatcher, pass it when invoking it: `getFriendsUseCase(input, dispatcher)`.

## `EmaResult<T, E>`

Like `kotlin.Result`, but the failure can be **any type**, not only `Throwable`. Use it as the return type of use cases
so the failures are part of the contract, and the side effect turns each one into an action:

```kotlin
getFriendsUseCase(input)
    .onSuccess { friends -> dispatch(HomeAction.EffectQueryResult.Loaded(friends)) }
    .onFailure { dispatch(HomeAction.EffectQueryResult.Failed) }
```

| Function                                           | Does                                         |
|----------------------------------------------------|----------------------------------------------|
| `EmaResult.success(data)` / `EmaResult.failure(e)` | Creates a result.                            |
| `isSuccess` / `isFailure`                          | Tells which one it is.                       |
| `getOrNull()`, `getOrDefault(x)`, `getOrThrow()`, `getFailureOrNull()` | Reads it.                |
| `map`, `flatMap`                                   | Transform the success.                       |
| `mapError`, `flatMapError`                         | Transform the failure.                       |
| `onSuccess`, `onFailure`                           | Side effects, returning the same result.     |
