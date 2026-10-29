# Use cases

A use case is one operation of the business logic. Each one has a single `Input`. What they do inside is up to the implementation: call repositories, other use cases,
platform services or just compute something.

The input `I` and the output `O` are **type parameters you choose**, and Emanate doesn't impose any: a use case can
return a plain model, `Unit`, a `Boolean`, or any type you define.

| Class                          | Use it for                                                                       |
|--------------------------------|----------------------------------------------------------------------------------|
| `EmaUseCase<I, O>`             | A `suspend` operation. Runs on the background dispatcher it receives in the constructor, by default the `io` one of the [configuration](../guides/configuration.md#dispatchers). |
| `EmaFlowUseCase<I, O>`         | An operation that emits values over time (`Flow<O>`). The flow runs on the dispatcher it receives in the constructor, by default the `io` one of the configuration, and the values reach the collector on its own dispatcher (`main` in a side effect). |
| `EmaSyncUseCase<I, O>`         | A fast, synchronous operation. It's an interface with no dispatcher: it runs on the thread that calls it. |

Implement `useCaseFunction` (the protected function) and call the use case like a function:

```kotlin
class GetGreetingUseCase(private val repository: UserRepository) :
    EmaUseCase<GetGreetingUseCase.Input, String>() {

    override suspend fun useCaseFunction(input: Input) = "Hello, ${repository.getUser(input.id).name}"

    data class Input(val id: String)
}

// In a side effect
val greeting = getGreetingUseCase(GetGreetingUseCase.Input(state.userId))
```

### Model the failures with `EmaResult`

When the operation can fail, [`EmaResult`](results.md) is a good type to return, and the recommended one. It puts the
failures in the signature, so whoever calls the use case can't forget them, and it lets you use your own error type
instead of exceptions. A good practice in the side effect is to turn each case into an action, so the
[reducers](../concepts/reducers.md) decide how the state changes:

```kotlin
class GetFriendsUseCase(private val repository: FriendsRepository) :
    EmaUseCase<GetFriendsUseCase.Input, EmaResult<List<User>, FriendsError>>() {

    override suspend fun useCaseFunction(input: Input) = repository.getFriends(input.query)

    data class Input(val query: String)
}

// In a side effect
getFriendsUseCase(GetFriendsUseCase.Input(state.query))
    .onSuccess { friends -> dispatch(HomeAction.EffectQueryResult.Loaded(friends)) }
    .onFailure { dispatch(HomeAction.EffectQueryResult.Failed) }
```

If the operation can't fail in a way the caller cares about, return the model directly, and don't wrap it for the sake
of it.

The side effects run on the main dispatcher, and the use cases switch to the background one themselves, so you don't
need to. To run a use case once on another dispatcher, pass it when invoking it: `getFriendsUseCase(input, dispatcher)`.

## Where to call them

The natural place is the [side effects](../concepts/side-effects.md): they run on `main` and call the use case, and
it's a good practice to dispatch an action with its result. But a use case is just a block that groups a complex operation behind a single `Input`, and
nothing ties it to the ViewModel. You can call it from anywhere that can call a `suspend` function: an initializer, a
repository, or another use case that composes several of them. Since `EmaUseCase` and `EmaFlowUseCase` switch to the
background dispatcher themselves, it's safe to call them from `main`.
