# Casos de uso

Las operaciones lógicas de la app viven en los casos de uso, y no son simples intermediarios para operaciones CRUD: un login puede llamar a varios repositorios y managers, y todo eso va en un caso de uso.

Un caso de uso es una operación de la lógica de negocio. Cada uno tiene un único `Input`. Lo que hacen por dentro
depende de la implementación: llamar a repositorios, a otros casos de uso, a servicios de la plataforma o simplemente
calcular algo.

La entrada `I` y la salida `O` son **parámetros de tipo que eliges tú**, y Emanate no impone ninguno: un caso de uso
puede devolver un modelo normal, `Unit`, un `Boolean` o cualquier tipo que definas.

| Clase                          | Úsala para                                                                       |
|--------------------------------|----------------------------------------------------------------------------------|
| `EmaUseCase<I, O>`             | Una operación `suspend`. Se ejecuta en el dispatcher de segundo plano que recibe en el constructor, por defecto el `io` de la [configuración](../guides/configuration.md#dispatchers). |
| `EmaFlowUseCase<I, O>`         | Una operación que emite valores a lo largo del tiempo (`Flow<O>`). El flow se ejecuta en el dispatcher que recibe en el constructor, por defecto el `io` de la configuración, y los valores llegan al colector en su propio dispatcher (`main` en un side effect). |
| `EmaSyncUseCase<I, O>`         | Una operación rápida y síncrona. Es una interfaz sin dispatcher: se ejecuta en el hilo que la llama. |

Elige la clase según cómo se ejecute la operación: síncrona, `suspend` o `Flow`. En `EmaUseCase` y `EmaFlowUseCase` implementa `useCaseFunction` (la función protegida), y en `EmaSyncUseCase` implementa `invoke`. Llama al caso de uso como a una función:

```kotlin
class GetGreetingUseCase(private val repository: UserRepository) :
    EmaUseCase<GetGreetingUseCase.Input, String>() {

    override suspend fun useCaseFunction(input: Input) = "Hola, ${repository.getUser(input.id).name}"

    data class Input(val id: String)
}

// En un side effect
val greeting = getGreetingUseCase(GetGreetingUseCase.Input(state.userId))
```

### Modela los fallos con `EmaResult`

Los errores controlados son tipos que modelas tú, no excepciones: una `data class` o una `sealed interface`. Cuando la operación puede fallar, [`EmaResult`](results.md) es un buen tipo de retorno, y el recomendado. Pone los fallos
en la firma, así que quien llama al caso de uso no puede olvidarlos, y te permite usar tu propio tipo de error en lugar
de excepciones. Una buena práctica en el side effect es convertir cada caso en una acción, para que los
[reducers](../concepts/reducers.md) decidan cómo cambia el estado:

```kotlin
class GetFriendsUseCase(private val repository: FriendsRepository) :
    EmaUseCase<GetFriendsUseCase.Input, EmaResult<List<User>, FriendsError>>() {

    override suspend fun useCaseFunction(input: Input) = repository.getFriends(input.query)

    data class Input(val query: String)
}

// En un side effect
getFriendsUseCase(GetFriendsUseCase.Input(state.query))
    .onSuccess { friends -> dispatch(HomeAction.SideEffects.FriendsLoaded(friends)) }
    .onFailure { dispatch(HomeAction.SideEffects.FriendsLoadFailed) }
```

Si la operación no puede fallar de un modo que le importe a quien llama, devuelve el modelo directamente y no lo
envuelvas por envolverlo.

Los side effects se ejecutan en el dispatcher principal, y los casos de uso cambian solos al de segundo plano, así que
tú no necesitas hacerlo. Para ejecutar un caso de uso una vez en otro dispatcher, pásalo al invocarlo:
`getFriendsUseCase(input, dispatcher)`.

## Dónde llamarlos

El lugar natural son los [side effects](../concepts/side-effects.md): se ejecutan en `main` y llaman al caso de uso, y es
una buena práctica enviar una acción con su resultado. Pero un caso de uso es solo un bloque que agrupa una operación
compleja tras un único `Input`, y nada lo ata al ViewModel. Puedes llamarlo desde cualquier sitio que pueda llamar a una
función `suspend`: un inicializador, un repositorio u otro caso de uso que componga varios. Como `EmaUseCase` y
`EmaFlowUseCase` cambian solos al dispatcher de segundo plano, es seguro llamarlos desde `main`.
