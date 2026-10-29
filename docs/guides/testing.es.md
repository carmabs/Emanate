# Testing

Como un ViewModel no tiene dependencias de Android, se testea como Kotlin normal: sin emulador y sin Robolectric.
Si tus ViewModels viven en un módulo multiplataforma, los mismos tests se ejecutan en todos los targets desde `commonTest`.
Todo el contrato de estado y eventos es observable:

- `dispatch(action)` es la entrada.
- `stateFlow.value` es el estado.
- `eventFlow` contiene los eventos pendientes.

## Testear un EmaViewModel con emanate-test

`emanate-test` tiene las utilidades para testear los ViewModels hechos con `EmaViewModel` y las
[sources](../concepts/source.es.md). Es una librería JVM, así que añádela a los tests de un módulo JVM o Android:

```kotlin
dependencies {
    testImplementation("com.github.carmabs.ema:emanate-test:$emanateVersion")
}
```

Incluye `kotlinx-coroutines-test` y necesita la misma versión de `emanate-core` con la que se compiló.

| Utilidad                        | Para qué                                                                      |
|---------------------------------|-------------------------------------------------------------------------------|
| `useTestConfiguration()`          | Ejecuta los ViewModels, las sources y los casos de uso creados después en el dispatcher del test, también como `Dispatchers.Main`. Llámalo antes de cada test. |
| `resetTestConfiguration()`        | Restaura los dispatchers y la configuración de Emanate. Llámalo después de cada test. |
| `testConfiguration(dispatcher)` | Una configuración que se ejecuta en el dispatcher del test, para pasársela a los ViewModels en lugar de llamar a `useTestConfiguration()`. Consulta [Sin cambiar la configuración de Emanate](#sin-cambiar-la-configuración-de-emanate). |
| `viewModel.record()`            | Graba en orden las acciones, los estados y los eventos de un ViewModel o una source. |
| `assertActions(...)`            | Las acciones grabadas, incluidas las que bloquea un middleware.               |
| `assertStates(...)`             | Todos los estados que devuelven los reducers.                                 |
| `assertState(state)` / `assertState { }` | El último estado, o una condición sobre él.                          |
| `assertEvents(...)` / `assertNoEvents()` | Los eventos publicados.                                              |
| `viewModel.assertSetup()`       | Falla si un middleware, reducer o side effect nunca se ejecuta, como los de una acción emitida que no se escucha. Consulta [Comprobar el setup](#comprobar-el-setup). |

```kotlin
class LoginViewModelTest {

    @BeforeTest
    fun setUp() {
        useTestConfiguration()
    }

    @AfterTest
    fun tearDown() {
        resetTestConfiguration()
    }

    @Test
    fun `the right credentials log in the user`() = runTest {
        val viewModel = LoginViewModel(LoginUseCase(FakeRepository()), LoginState.DEFAULT)
        val recorder = viewModel.record()

        viewModel.dispatch(LoginAction.UserNameWritten("Admin"))
        viewModel.dispatch(LoginAction.PasswordWritten("1234"))
        viewModel.dispatch(LoginAction.Login)
        advanceUntilIdle()

        recorder.assertState { !it.isLoading }
        recorder.assertEvents(LoginEvent.Message("Carmabs"), LoginEvent.LoginSuccess(admin))
    }
}
```

- **Crea el ViewModel después de `useTestConfiguration`**, para que se ejecute en el dispatcher del test. Por defecto es
  un `UnconfinedTestDispatcher`, que procesa las acciones en cuanto se despachan; pasa un `StandardTestDispatcher` para
  procesarlas cuando avance el test.
- **Graba antes de la primera acción** para grabarlo todo.
- **`advanceUntilIdle()`** ejecuta los side effects, retardos incluidos, porque `runTest` usa el scheduler del
  dispatcher del test.
- **Se pueden testear varios ViewModels** juntos, por ejemplo los resultados emitidos entre pantallas: el
  `LoginViewModelTest` del sample crea los ViewModels de creación de perfil, home y login, y comprueba que el usuario
  creado en el primero llega al último.

### Sin cambiar la configuración de Emanate

`useTestConfiguration()` cambia la configuración de Emanate y `Dispatchers.Main`, que son compartidos por todo el proceso,
así que los tests que lo usan no se pueden ejecutar en paralelo en la misma JVM. Si tus ViewModels reciben la
configuración en el constructor (consulta [Configuración explícita](configuration.es.md#configuración-explícita)),
pásales `testConfiguration(dispatcher)` en su lugar: no cambia nada compartido, así que no hay nada que restaurar.

```kotlin
class LoginViewModel(
    private val loginUseCase: LoginUseCase,
    initialState: LoginState,
    configuration: EmaConfiguration = Emanate.configuration
) : EmaViewModel<LoginState, LoginAction, LoginEvent>(initialState, configuration)

class LoginViewModelTest {

    @Test
    fun `the right credentials log in the user`() = runTest {
        val viewModel = LoginViewModel(
            LoginUseCase(FakeRepository()),
            LoginState.DEFAULT,
            testConfiguration(UnconfinedTestDispatcher(testScheduler))
        )
        val recorder = viewModel.record()
        // ...
    }
}
```

- **Crea el dispatcher con `testScheduler`**, el scheduler de `runTest`, para que `advanceUntilIdle()` ejecute el
  trabajo del ViewModel. `Dispatchers.Main` no se sustituye, así que `runTest` no lo toma de ahí.
- El resto de la configuración es la de por defecto, `EmaConfiguration()`, no la de Emanate. Pasa otra como segundo
  parámetro para cambiarla: `testConfiguration(dispatcher, EmaConfiguration(logger = null))`.
- Los [casos de uso](utilities.es.md#casos-de-uso) se ejecutan en el dispatcher `io` de la configuración de Emanate
  salvo que reciban otro en el constructor: usa casos de uso falsos, o pásales el dispatcher del test.
- **Cada `testConfiguration` tiene su propio broadcaster**, así que las acciones que se emiten en un test no llegan a
  los demás. Los ViewModels de un test que se envían resultados tienen que recibir la misma configuración: créala una
  sola vez.

### Comprobar el setup

Un reducer, middleware o side effect para una acción que el ViewModel nunca recibe no falla: simplemente no se
ejecuta. Ocurre cuando se gestiona una acción emitida pero falta `listen<Tipo>()` en el bloque `broadcasts`.
`assertSetup()` comprueba que cada pieza puede recibir acciones de su tipo: acciones de la pantalla, inicializadores,
acciones del ciclo de vida o acciones emitidas que escucha.

```kotlin
@Test
fun `the setup is complete`() {
    HomeViewModel(GetUserFriendsUseCase(FakeRepository()), HomeState.DEFAULT).assertSetup()
}
```

Falla con todas las piezas que nunca se ejecutan:

```
The reducer of UserCreated never runs: HomeViewModel doesn't receive UserCreated. If it is broadcast by another
source, add listen<UserCreated>() in the broadcasts block.
```

Solo se informa de los tipos que el ViewModel seguro que no puede recibir. Una acción de la pantalla puede implementar
cualquier interfaz, así que no se informa de una pieza para una interfaz que no está relacionada con las acciones de la
pantalla.

## Testear los ViewModels clásicos

Añade `kotlinx-coroutines-test` a tus dependencias de test:

```kotlin
class CounterViewModelTest {

    private fun TestScope.createViewModel() =
        CounterViewModel(UnconfinedTestDispatcher(testScheduler)).apply {
            onCreated()
        }

    @Test
    fun `increment action updates the state`() = runTest {
        val viewModel = createViewModel()

        viewModel.dispatch(CounterAction.Increment)

        assertEquals(CounterState(count = 1), viewModel.stateFlow.value)
    }

    @Test
    fun `reaching the limit posts an event`() = runTest {
        val viewModel = createViewModel()

        repeat(10) { viewModel.dispatch(CounterAction.Increment) }

        assertEquals(
            listOf<CounterEvent>(CounterEvent.LimitReached(10)),
            viewModel.eventFlow.first()
        )
    }
}
```

Los tests de la propia librería, en `emanate-core/src/commonTest`, tienen más ejemplos: side effects, eventos, resultados
entre pantallas y el ciclo de vida de la vista.

### Lo que conviene saber

1. **Pasa tu propio dispatcher.** El parámetro `dispatcher` de `EmaViewModelClassic` / `EmaViewModelAction` es el
   `CoroutineDispatcher` donde se ejecuta el ViewModel, que crea su propio scope con él. Exponlo en el constructor de
   tus ViewModels, con un valor por defecto, para que los tests puedan sustituirlo. El de por defecto es el dispatcher
   `main` de la configuración, `Dispatchers.Main`, que no existe en los tests unitarios. Con un
   `UnconfinedTestDispatcher` el trabajo se ejecuta inmediatamente; con un `StandardTestDispatcher`, llama a
   `advanceUntilIdle()` para ejecutarlo.

   ```kotlin
   class CounterViewModel(dispatcher: CoroutineDispatcher = Emanate.configuration.dispatchers.main) :
       EmaViewModelAction<CounterState, CounterAction, CounterEvent>(CounterState(), dispatcher = dispatcher)
   ```

   El segundo parámetro es la [configuración](configuration.es.md#configuración-explícita), así que pasa el dispatcher
   por nombre, o expón la configuración entera.
2. **No hace falta configuración.** Si no se llama a `Emanate.init`, se usa la configuración por defecto. No llames a
   `Emanate.init` en los tests: solo se puede llamar una vez por proceso.
3. **Llama a `onCreated()`** para ejecutar `onStateCreated(initializer)`. Pasa un inicializador si la pantalla lo
   necesita: `viewModel.onCreated(MyInitializer.Default("x"))`.
4. **Usa casos de uso falsos.** Son clases normales: pasa una implementación falsa, o usa tu librería de mocks.
5. **La vista consume los eventos.** En un test, léelos con `eventFlow.first()` y llama a `consumeEvent(event)` para
   simular que la vista los ha gestionado.
6. **El tiempo.** El dispatcher de test se salta los `delay` dentro de un `sideEffect`, así que los debounces y los
   timeouts se ejecutan al instante.

## Qué testear

| Test                                                 | Cómo                                                       |
|------------------------------------------------------|------------------------------------------------------------|
| Una acción cambia el estado                          | `dispatch` y comprueba `stateFlow.value`.                  |
| Una acción publica un evento                         | `dispatch` y comprueba `eventFlow.first()`.                |
| Un fallo muestra el diálogo o error correcto         | Caso de uso falso que falla; comprueba el campo del estado. |
| Se aplica el inicializador                           | `onCreated(initializer)` y comprueba el estado.            |
| Las propiedades derivadas del estado                 | Tests unitarios normales de la data class.                 |

## Testear las pantallas

El contenido de una pantalla Compose no tiene estado: `onState(state, actions)` solo pinta lo que recibe. Píntalo con
cualquier estado (consulta [Previews](../compose/screens.es.md#previews)) y compruébalo con tests de UI de Compose o con
tests de capturas, sin ViewModel.
