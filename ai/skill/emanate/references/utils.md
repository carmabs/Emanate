# Utilities: use cases, EmaResult, EmaText

## Use cases

The logical operations of the app live here. They are **not** just intermediaries for CRUD: a login can call several repositories and managers, and all of that belongs in one use case.

Choose the class by how the operation runs:

| Operation | Class | Implement |
|---|---|---|
| Asynchronous (`suspend`) | `EmaUseCase<I, O>` | `override suspend fun useCaseFunction(input: I): O` |
| Reactive (`Flow`) | `EmaFlowUseCase<I, O>` | `override fun useCaseFunction(input: I): Flow<O>` |
| Synchronous (no `suspend`) | `EmaSyncUseCase<I, O>` | `override fun invoke(input: I): O` |

```kotlin
class LoginUseCase(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val sessionManager: SessionManager
) : EmaUseCase<LoginUseCase.Input, EmaResult<User, LoginError>>() {

    override suspend fun useCaseFunction(input: Input): EmaResult<User, LoginError> =
        authRepository.login(input.name, input.password).flatMap { token ->
            sessionManager.save(token)
            profileRepository.getUser(token)
        }

    data class Input(val name: String, val password: String)
}

loginUseCase(LoginUseCase.Input(name = state.name, password = state.password))   // from a side effect (main); it switches thread itself
```

One `Input` data class inside the use case, with named values; add fields with defaults. `EmaUseCase` and `EmaFlowUseCase` run on the configured `io` dispatcher (constructor param to override).

## Errors and EmaResult<T, E>

Controlled errors are **modelled types, not exceptions**. `data-XXX` turns library exceptions into them.

```kotlin
sealed interface LoginError {
    data object WrongCredentials : LoginError
    data class Server(val code: Int) : LoginError
}
```

A use case doesn't always return `EmaResult`, but it is recommended whenever it can return an error. If it can't fail in a way the caller cares about, return the model directly.
`EmaResult.success(x)` / `.failure(e)`; `onSuccess`, `onFailure`, `map`, `flatMap`, `getOrNull`, `getOrDefault`, `getFailureOrNull`, `getOrThrow`, `isSuccess`, `isFailure`.
In a side effect turn each case into an action: `.onSuccess { dispatch(LoginAction.SideEffects.UserLoggedIn(it)) }.onFailure { dispatch(LoginAction.SideEffects.UserLoginFailed(it)) }`.

## EmaText (texts in the state)

Never put translated `String`s in state (ViewModel is platform-free, language can change, tests would compare translations). Hold a description, the view resolves it:

```kotlin
data class HomeState(val title: EmaText, val error: EmaText?) : EmaState     // DEFAULT: title = EmaText.empty(), error = null
state.copy(title = EmaText.id(HOME_TITLE, user.name))          // EmaText.plural(FRIENDS, 3, 3), EmaText.text("Ana")
recorder.assertState { it.title == EmaText.id(HOME_TITLE, "Ana") }   // test the decision, not the translation
```
On Android the `id` is a resource id: `R` only exists in Android modules, so a pure-Kotlin ViewModel must not reference `R`. Create the text where `R` exists (`R.string.welcome.toEmaText(name)`, `R.plurals.friends.toEmaText(n, n)`) or keep platform-neutral ids in the ViewModel's module and map them in the UI.
Resolve in a composable with `text.stringResource()` (`emanate-compose-android`), or `text.string(context)` (`emanate-android`). Equal descriptions are equal `EmaText`s.
Strings in `values/` + `values-xx/`, prefixed by screen (`home_title`).
