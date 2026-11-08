# `EmaResult<T, E>`

Como `kotlin.Result`, pero el fallo puede ser **de cualquier tipo**, no solo `Throwable`. Hace que los fallos formen
parte del contrato de una función: en lugar de lanzar una excepción, la función devuelve un éxito con los datos o un
fallo con un error que has modelado.

```kotlin
sealed interface FriendsError {
    data object NoConnection : FriendsError
    data class Server(val code: Int) : FriendsError
}

fun getFriends(query: String): EmaResult<List<User>, FriendsError>
```

Puedes devolverlo desde cualquier capa, no solo desde los [casos de uso](use-cases.md): un repositorio, una fuente de
datos o un mapper también pueden usarlo.

## Convierte un resultado en acciones

Una buena práctica en un [side effect](../concepts/side-effects.md) es convertir cada caso del `EmaResult` que devuelve
un caso de uso en una acción, para que los reducers gestionen el resultado como cualquier otro cambio del estado. No es
obligatorio: el resultado también se puede consumir en cualquier otro sitio.

```kotlin
getFriendsUseCase(input)
    .onSuccess { friends -> dispatch(HomeAction.SideEffects.FriendsLoaded(friends)) }
    .onFailure { error -> dispatch(HomeAction.SideEffects.FriendsLoadFailed(error)) }
```

## Funciones

Los ejemplos usan estos dos resultados:

```kotlin
val ok: EmaResult<Int, String> = EmaResult.success(3)
val ko: EmaResult<Int, String> = EmaResult.failure("no connection")
```

### Comprobar y leer

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
ko.getOrThrow()               // lanza IllegalArgumentException("no connection")
                              // (si el fallo es un Throwable, lanza ese)
```

### Transformar el éxito

`map` y `flatMap` solo ejecutan su bloque cuando el resultado es un éxito. Un fallo pasa sin tocarse.

```kotlin
ok.map { it * 2 }             // Success(6)
ko.map { it * 2 }             // Failure("no connection"), el bloque no se ejecuta

// flatMap encadena una operación que también puede fallar
ok.flatMap { if (it > 0) EmaResult.success("$it") else EmaResult.failure("negative") }
                              // Success("3")
ko.flatMap { EmaResult.success("$it") }
                              // Failure("no connection"), el bloque no se ejecuta
```

### Transformar el fallo

`mapError` y `flatMapError` solo ejecutan su bloque cuando el resultado es un fallo. Un éxito pasa sin tocarse.

```kotlin
ko.mapError { FriendsError.NoConnection }   // Failure(NoConnection)
ok.mapError { FriendsError.NoConnection }   // Success(3), el bloque no se ejecuta

// flatMapError intenta recuperarse con otro resultado, por ejemplo una caché
ko.flatMapError { EmaResult.success(0) }    // Success(0)
ok.flatMapError { EmaResult.success(0) }    // Success(3), el bloque no se ejecuta
```

### Transformar todo el resultado

```kotlin
// mapResult recibe el resultado, éxito o fallo, y devuelve otro
ok.mapResult { result -> EmaResult.success<String, String>(result.getOrDefault(-1).toString()) }
                              // Success("3")
ko.mapResult { result -> EmaResult.success<String, String>(result.getOrDefault(-1).toString()) }
                              // Success("-1")
```

### Reaccionar sin cambiarlo

`onSuccess` y `onFailure` ejecutan el bloque solo en su caso y devuelven el mismo resultado, así que se pueden encadenar.

```kotlin
ok.onSuccess { println("got $it") }         // imprime "got 3", devuelve Success(3)
ko.onSuccess { println("got $it") }         // no imprime nada, devuelve Failure("no connection")

ko.onFailure { println("failed: $it") }     // imprime "failed: no connection", devuelve el mismo Failure

ok.onSuccess { println("got $it") }
  .onFailure { println("failed: $it") }     // solo imprime el primero
```

### Combinarlas

Cada función devuelve un `EmaResult`, así que se encadenan. Por ejemplo, transforma los datos de un repositorio en el
modelo de la pantalla y el error en el tipo que expone tu caso de uso:

```kotlin
repository.getFriends(query)                       // EmaResult<List<UserDto>, HttpError>
    .map { dtos -> dtos.map { it.toUser() } }      // EmaResult<List<User>, HttpError>
    .mapError { error -> error.toFriendsError() }  // EmaResult<List<User>, FriendsError>
```

Recupérate de un fallo con una alternativa:

```kotlin
remote.getFriends(query)
    .flatMapError { local.getFriends(query) }      // si falla el servidor, usa la caché
```

## Desde `kotlin.Result`

`Result<S>.toEmaResult { throwable -> error }` convierte un `Result`, transformando la excepción en tu tipo de fallo:

```kotlin
runCatching { api.getFriends() }
    .toEmaResult { throwable -> FriendsError.Server(code = 500) }
```
