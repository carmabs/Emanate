# Utilities: use cases, EmaResult, EmaText

## Use cases

| Class | For |
|---|---|
| `EmaUseCase<I, O>` | `suspend` operation, runs on the configured `io` dispatcher (constructor param to override) |
| `EmaFlowUseCase<I, O>` | Emits `Flow<O>` over time |
| `EmaSyncUseCase<I, O>` | Fast synchronous operation, no dispatcher |

```kotlin
class GetFriendsUseCase(private val repository: FriendsRepository) :
    EmaUseCase<GetFriendsUseCase.Input, EmaResult<List<User>, FriendsError>>() {
    override suspend fun useCaseFunction(input: Input) = repository.getFriends(input.query)
    data class Input(val query: String, val onlyOnline: Boolean = false)   // named values, add fields with defaults
}
getFriendsUseCase(GetFriendsUseCase.Input(query = state.query))             // from a side effect (main); it switches thread itself
```

One operation per use case. Safe to call from `main`.

## EmaResult<T, E>

Like `kotlin.Result` but the failure is any type you model (`sealed interface FriendsError`). Return it from use cases/repositories when the caller must handle failure; return the plain model when failure is not the caller's concern.
`EmaResult.success(x)` / `.failure(e)`; `onSuccess`, `onFailure`, `map`, `flatMap`, `getOrNull`, `getOrDefault`, `getFailureOrNull`, `getOrThrow`, `isSuccess`, `isFailure`.
In a side effect turn each case into an action: `.onSuccess { dispatch(Loaded(it)) }.onFailure { dispatch(Failed(it)) }`.

## EmaText (texts in the state)

Never put translated `String`s in state (ViewModel is platform-free, language can change, tests would compare translations). Hold a description, the view resolves it:

```kotlin
data class HomeState(val title: EmaText = EmaText.empty(), val error: EmaText? = null) : EmaState
state.copy(title = EmaText.id(HOME_TITLE, user.name))          // EmaText.plural(FRIENDS, 3, 3), EmaText.text("Ana")
recorder.assertState { it.title == EmaText.id(HOME_TITLE, "Ana") }   // test the decision, not the translation
```
On Android the `id` is a resource id: `R` only exists in Android modules, so a pure-Kotlin ViewModel must not reference `R`. Create the text where `R` exists (`R.string.welcome.toEmaText(name)`, `R.plurals.friends.toEmaText(n, n)`) or keep platform-neutral ids in the ViewModel's module and map them in the UI.
Resolve in a composable with `text.stringResource()` (`emanate-compose-android`), or `text.string(context)` (`emanate-android`). Equal descriptions are equal `EmaText`s.
Strings in `values/` + `values-xx/`, prefixed by screen (`home_title`).
