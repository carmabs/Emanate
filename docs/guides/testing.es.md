# Testing

Un `EmaViewModel` no tiene dependencias de plataforma, así que lo testeas como Kotlin puro, sin dispositivo ni
emulador. Y como cada cambio pasa por su `EmaSource`, un test puede ver **todo** lo que hizo la pantalla, en orden: cada
acción, cada estado y cada evento.

| Controlas                                          | Compruebas                                        |
|----------------------------------------------------|---------------------------------------------------|
| Las acciones, con `dispatch` y `onCreated`         | Los estados que devuelven los reducers, en orden  |
| Las piezas que recibe: fakes, mocks, retrasos      | Los eventos publicados, en orden                  |
| El tiempo, con el reloj virtual de `runTest`       | Las acciones, incluidas las que detuvo un middleware |

## Qué testear

| Test                                                 | Cómo                                                       |
|------------------------------------------------------|------------------------------------------------------------|
| Una acción cambia el estado                          | `dispatch`, y después `assertStates` o `assertState`.      |
| Una acción publica un evento                         | `dispatch`, y después `assertEvents`.                      |
| Un fallo muestra el diálogo / error correcto         | Inyecta un mock que falla, comprueba el campo del estado.  |
| Una respuesta lenta mantiene el estado de carga      | Inyecta un mock con latencia, `advanceTimeBy`, comprueba el estado. |
| Se aplica el inicializador                           | `onCreated(initializer)`, comprueba el estado.             |
| Todas las piezas pueden ejecutarse                   | `assertSetup()`.                                           |
| Las propiedades derivadas del estado                 | Tests unitarios normales sobre la data class.              |

## Testear un EmaViewModel con emanate-test

`emanate-test` tiene las utilidades para testear `EmaViewModel` y [`EmaSource`](../concepts/source.md). Añádelo a los
tests unitarios del módulo de tus ViewModels (por ahora se publica para los source sets de test de la JVM):

```kotlin
dependencies {
    testImplementation("com.github.carmabs.ema:emanate-test:$emanateVersion")
}
```

Trae `kotlinx-coroutines-test` y requiere la misma versión de `emanate-core` con la que se construyó.

Estas son todas las utilidades de `emanate-test`. Cada una se explica más abajo, con un ejemplo, por qué importa y qué
pasa si no la usas.

| Utilidad                        | En una línea                                                                  |
|---------------------------------|-------------------------------------------------------------------------------|
| [`useTestConfiguration()`](#instalar-la-configuración-de-test) | Instala en Emanate una configuración que ejecuta los ViewModels, `EmaSource` y casos de uso en el dispatcher del test. |
| [`resetTestConfiguration()`](#instalar-la-configuración-de-test) | Deshace la anterior, para que un test no afecte al siguiente. |
| [`testConfiguration(dispatcher)`](#sin-cambiar-la-configuración-de-emanate) | Lo mismo, pero se pasa al ViewModel en lugar de cambiar lo compartido. |
| [`viewModel.record()`](#grabar-un-viewmodel) | Empieza a grabar las acciones, estados y eventos. |
| [`assertActions(...)`](#comprobar-lo-grabado) | Las acciones grabadas, incluidas las bloqueadas por un middleware. |
| [`assertStates(...)`](#comprobar-lo-grabado) | Cada estado devuelto por los reducers, en orden. |
| [`assertState(state)` / `assertState { }`](#comprobar-lo-grabado) | El último estado, o una condición sobre él. |
| [`assertEvents(...)` / `assertNoEvents()`](#comprobar-lo-grabado) | Los eventos publicados. |
| [`viewModel.assertSetup()`](#comprobar-el-setup) | Falla si un middleware, reducer o side effect nunca se ejecuta. |
| [`testStateStore()` / `restoreStateOf(...)`](#testear-la-muerte-del-proceso) | Un store en memoria para testear lo que un ViewModel guarda y restaura. |

## Instalar la configuración de test

`useTestConfiguration()` y `resetTestConfiguration()` van juntos: uno antes de cada test, el otro después.

**Qué hace `useTestConfiguration()`.** Cambia `Emanate.configuration`, compartida por todo el proceso, de modo que los
ViewModels, `EmaSource` y casos de uso creados después se ejecutan en el dispatcher del test, un `TestDispatcher` de
`kotlinx-coroutines-test`. Mantiene el resto de la configuración actual (logger, política de errores...) y solo
sustituye los dispatchers. También sustituye `Dispatchers.Main` por él (`Dispatchers.setMain`), porque en los tests
unitarios no hay un `Main` real, y así `runTest` usa el mismo scheduler. Por defecto es un `UnconfinedTestDispatcher`,
que procesa cada acción en cuanto se envía. Devuelve el dispatcher.

**Por qué importa.** El trabajo de un ViewModel (reducers, side effects, retrasos, casos de uso) es asíncrono. En el
dispatcher del test se ejecuta bajo el reloj virtual de `runTest`: `advanceUntilIdle()` lo termina, y un
`delay(3.seconds)` no tarda nada. El test es determinista y rápido.

**Qué pasa sin él.**

- Un ViewModel usa `Dispatchers.Main`, que no existe en los tests unitarios de la JVM, así que falla con
  `Module with the Main dispatcher had failed to initialize`.
- Aunque sustituyas `Main` tú mismo, los side effects y casos de uso se ejecutarían en el dispatcher `io` real, en otros
  hilos. `runTest` no espera por ellos, así que las aserciones se ejecutan antes de que termine el trabajo y el test
  pasa o falla al azar. Un `delay` real lo hace tan lento como el retraso.

**Qué hace `resetTestConfiguration()`.** Restaura `Dispatchers.Main` y la configuración de Emanate, y elimina las
acciones emitidas con `keepUntilReceived` que nadie recibió.

**Qué pasa sin él.** Los siguientes tests empiezan con el `Main` y la configuración de este, y con sus acciones de
broadcast pendientes: un test puede recibir una acción que envió otro test. Los fallos dependen del orden de los tests,
que es el tipo más difícil de encontrar.

**Cómo usarlo.**

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

- **Crea el ViewModel después de `useTestConfiguration()`**, porque lee la configuración cuando se crea.
- **`advanceUntilIdle()`** ejecuta los side effects, retrasos incluidos, porque `runTest` usa el scheduler del
  dispatcher del test.

### Avanzado: elegir cuándo se ejecuta el trabajo

No necesitas esto para escribir tus primeros tests. `useTestConfiguration()` y `testConfiguration()` aceptan cualquier
`TestDispatcher`, y los dos que puedes elegir se diferencian en **cuándo se ejecuta el trabajo que se lanza**:

| | `UnconfinedTestDispatcher` (por defecto) | `StandardTestDispatcher` |
|---|---|---|
| Cuándo se ejecuta el trabajo | En cuanto se lanza | Cuando el test avanza: `advanceUntilIdle()`, `advanceTimeBy()` o `runCurrent()` |
| Tras `dispatch(action)` | El reducer ya se ha ejecutado, y el side effect también si no espera | Nada se ha ejecutado: la acción está en una cola |
| Dos `dispatch` seguidos | El primero termina antes de que empiece el segundo | Ambos se encolan y se procesan juntos cuando el test avanza |
| Úsalo para | La mayoría de tests: enviar, avanzar y comprobar el resultado | Comprobar lo que pasa **entre** la acción y su resultado, o el orden de varias acciones |

Con `StandardTestDispatcher` el test controla el momento, así que puedes comprobar que algo **todavía no** ha pasado:

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
    recorder.assertActions()                                   // todavía esperando en la cola

    advanceUntilIdle()
    recorder.assertActions(HomeAction.RefreshRequested, HomeAction.SideEffects.QueryLoaded(listOf(ana)))
}
```

- **¿Por qué no usar siempre `StandardTestDispatcher`?** Porque cada test tendría que avanzar después de cada acción,
  incluso los que no se preocupan por el orden. `UnconfinedTestDispatcher` es el valor por defecto para mantener corto
  el test habitual.
- **¿Por qué usarlo?** Se parece más a producción, donde `Dispatchers.Main` no ejecuta el trabajo al instante sino que lo
  encola. Un test con dos acciones seguidas puede fallar con él y pasar con el unconfined, porque el unconfined esconde
  un problema de orden.
- Con `testConfiguration`, créalo con el scheduler del test: `testConfiguration(StandardTestDispatcher(testScheduler))`.

### Sin cambiar la configuración de Emanate

`useTestConfiguration()` cambia la configuración de Emanate y `Dispatchers.Main`, que comparte todo el proceso, así que
los tests que lo usan no pueden ejecutarse en paralelo en el mismo proceso. Si tus ViewModels reciben la configuración en
el constructor (mira [Configuración explícita](configuration.md#configuración-explícita)), pásales
`testConfiguration(dispatcher)` en su lugar: no cambia nada compartido, así que no hay nada que restablecer.

**Qué hace.** Devuelve una `EmaConfiguration` cuyos dispatchers `main`, `io` y `default` son el que le das. El resto es
la `EmaConfiguration()` por defecto, no la de Emanate, así que el test no depende de `Emanate.init`. También da a la
configuración su propio broadcaster.

**Por qué importa.** No cambia nada compartido, así que los tests pueden ejecutarse en paralelo y no hay `@AfterTest`.
Cada test tiene su dispatcher y su broadcaster.

**Qué pasa sin ello.** Si no usas ni esto ni `useTestConfiguration()`, el ViewModel obtiene `Emanate.configuration` y se
ejecuta con los dispatchers reales, con los problemas explicados arriba. Si compartes el broadcaster de Emanate entre
tests que se ejecutan en paralelo, las acciones emitidas en uno llegan a los ViewModels de los otros.

**Cómo usarlo.**

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

- **Crea el dispatcher con `testScheduler`**, el scheduler de `runTest`, para que `advanceUntilIdle()` ejecute el
  trabajo del ViewModel y de los casos de uso. `Dispatchers.Main` no se sustituye, así que `runTest` no lo toma de ahí.
- Pasa otra configuración como segundo parámetro para cambiar el resto:
  `testConfiguration(dispatcher, EmaConfiguration(logger = null))`.
- Los [casos de uso](../utils/use-cases.md) se ejecutan en el dispatcher `io` de la configuración de Emanate a menos que
  reciban otro en el constructor, como hace `GetUnreadCountUseCase`: pásales el mismo dispatcher que a la configuración,
  o simúlalos. Si no, se ejecutan en el dispatcher `io` real, fuera del control del test.
- **Cada `testConfiguration` tiene su propio broadcaster**, así que las acciones emitidas en un test no llegan a los
  otros. Los ViewModels de un test que se envían resultados entre sí deben recibir la misma configuración: créala una
  vez.

## Grabar un ViewModel

`viewModel.record()` (y `emaSource.record()` para un [`EmaSource`](../concepts/source.md)) empieza a grabar y devuelve
un `EmaRecorder`.

**Qué hace.** Desde ese momento, guarda en orden cada acción que entra en el `EmaSource`, cada estado devuelto por un
reducer y cada evento publicado. El recorder los expone como listas: `recorder.actions`, `recorder.states` y
`recorder.events`.

**Por qué importa.** El ViewModel solo expone su estado **actual**. Un test que lo lee no puede ver que la pantalla pasó
por `isLoading = true` antes de mostrar los amigos, que se publicó un evento, o que un middleware bloqueó una acción. El
recorder es lo que te permite comprobar el comportamiento, no solo el resultado final.

**Qué pasa sin él.** Solo puedes leer el último estado con `viewModel.stateFlow.value`. Un bug que muestra un spinner que
no desaparece en el momento adecuado, un evento duplicado o uno que falta, pasa desapercibido.

**Cómo usarlo.**

```kotlin
val viewModel = HomeViewModel(GetFriendsUseCase(FakeRepository()), HomeState())
val recorder = viewModel.record()           // antes de la primera acción

viewModel.dispatch(HomeAction.RefreshRequested)
advanceUntilIdle()

println(recorder.actions)   // [RefreshRequested, Loaded([Ana])]
println(recorder.states)    // [HomeState(isLoading=true), HomeState(friends=[Ana])]
println(recorder.events)    // [FriendsUpdated(1)]
```

- **Graba antes de la primera acción.** Lo que pasó antes de `record()` no se graba.
- **`states` no incluye el estado inicial**, solo los que devuelven los reducers.

## Comprobar lo grabado

El recorder compara **la lista completa, en orden**. Si hay un elemento de más, de menos o fuera de sitio, el test falla
y el mensaje muestra ambas listas. No escribes los bucles ni las comparaciones.

| Función | Comprueba | Ejemplo |
|---------|-----------|---------|
| `assertActions(...)` | Las acciones que entraron, en orden, incluidas las enviadas por side effects y las que bloqueó un middleware. | `recorder.assertActions(HomeAction.RefreshRequested, HomeAction.SideEffects.QueryLoaded(listOf(ana)))` |
| `assertStates(...)` | Los estados que devolvieron los reducers, uno por cada acción reducida. | `recorder.assertStates(HomeState(isLoading = true), HomeState(friends = listOf(ana)))` |
| `assertState(state)` | El último estado es exactamente ese. | `recorder.assertState(HomeState(friends = listOf(ana)))` |
| `assertState { }` | El último estado cumple una condición, sin escribir el estado completo. | `recorder.assertState { !it.isLoading && it.friends.size == 1 }` |
| `assertEvents(...)` | Los eventos publicados, en orden. | `recorder.assertEvents(HomeEvent.FriendsUpdated(1))` |
| `assertNoEvents()` | No se publicó ningún evento. | `recorder.assertNoEvents()` |

**Por qué importa.**

- `assertActions` es la única forma de saber que un middleware **bloqueó** una acción, porque una acción bloqueada no
  cambia el estado.
- `assertStates` comprueba el **camino**, no solo el destino: primero la carga, después el resultado.
- `assertState { }` mantiene el test corto y robusto, porque no se rompe cuando cambia un campo del estado que no tiene
  relación.
- `assertNoEvents()` es la comprobación de que algo **no** ocurrió: una carga fallida no debe publicar el evento de
  éxito.

**Qué pasa sin ellas.** Comparas `recorder.states` y `recorder.events` a mano. Funciona, pero cada test repite el mismo
código, y un test que olvida comprobar los eventos pasa cuando se publica uno equivocado. Una comprobación también falla
con un mensaje claro:

```
Expected actions <[Loaded(name=Ema)]> but were <[Load]>
The last state <ProfileState(name=, loading=true)> doesn't fulfil the condition
No state has been recorded
```

El último significa que no se ejecutó ningún reducer: la acción nunca se envió, se bloqueó, o el test no avanzó con
`advanceUntilIdle()`.

El ViewModel también puede recibir acciones desde fuera: el ciclo de vida, el inicializador y los resultados de otras
pantallas son acciones, así que un test las envía igual que lo hace la pantalla.

```kotlin
viewModel.onCreated(HomeInitializer.HomeUser(ana))   // EmaInitializer
viewModel.onResumeView()                             // EmaLifecycle.Resumed
```

## Comprobar el setup

`viewModel.assertSetup()` comprueba que cada middleware, reducer y side effect del ViewModel puede recibir acciones de
su tipo: acciones de la pantalla, inicializadores, acciones del ciclo de vida, las acciones de sus plugins o las
acciones de broadcast que escucha.

**Por qué importa.** Un reducer, middleware o side effect para una acción que el ViewModel nunca recibe no falla:
simplemente nunca se ejecuta. Ocurre cuando se gestiona una acción de broadcast pero falta `listen<Tipo>()` en el bloque
`broadcasts`.

**Qué pasa sin él.** Nada falla, ni en compilación ni en ejecución. El usuario crea un amigo en otra pantalla y la lista
nunca se actualiza, y ningún test lo detecta, porque los tests del ViewModel envían sus acciones directamente.

**Cómo usarlo.** Llámalo sobre el ViewModel, en un test propio. No necesita acciones, ni recorder, ni
`advanceUntilIdle()`: solo lee cómo está declarado el ViewModel.

```kotlin
@Test
fun `the setup is complete`() {
    HomeViewModel(GetFriendsUseCase(FakeRepository()), HomeState()).assertSetup()
}
```

Si cada pieza puede recibir su acción, el test pasa. Para ver qué detecta, toma un `HomeViewModel` que gestiona el
broadcast `FriendCreated` de la pantalla de perfil (mira [Broadcasts](../concepts/broadcasts.md)) pero olvidó
escucharlo:

```kotlin
class HomeViewModel(/* ... */) : EmaViewModel<HomeState, HomeAction, HomeEvent>(/* ... */) {

    override fun setup() = source {
        // broadcasts { listen<ProfileBroadcast.FriendCreated>() }   <- falta

        reducers {
            add<ProfileBroadcast.FriendCreated> { action -> state.copy(friends = state.friends + action.friend) }
        }
    }
}
```

El reducer está declarado, pero `HomeViewModel` nunca recibe `FriendCreated`, así que nunca se ejecuta y la lista de
inicio nunca muestra al nuevo amigo. Nada falla al ejecutar la app, pero `assertSetup()` sí, y dice qué pieza es y cómo
arreglarlo:

```
The reducer of FriendCreated never runs: HomeViewModel doesn't receive FriendCreated. If it is broadcast by another
source, add listen<FriendCreated>() in the broadcasts block.
```

Añadir `listen<ProfileBroadcast.FriendCreated>()` al bloque `broadcasts` hace que el test pase.

Solo se reportan los tipos que el ViewModel seguro no puede recibir. Una interfaz puede estar implementada por una
acción de la pantalla, así que no se reporta una pieza para una interfaz sin relación con las acciones de la pantalla.

## Testear la muerte del proceso

`testStateStore()` y `restoreStateOf(...)` son para los ViewModels con un [backup](process-death.md) de su estado.

### `testStateStore()`

**Qué hace.** Crea un `EmaStateStore` en memoria. Vive más que los ViewModels que lo usan, igual que el store de la
plataforma sobrevive a la muerte del proceso. `store.values` muestra lo que se ha guardado, por clave.

**Por qué importa.** Para testear que el formulario sobrevive, pasas el mismo store a dos ViewModels: el primero guarda,
el segundo, creado después, restaura. Esa es la muerte del proceso, sin dispositivo.

**Qué pasa sin él.** Tendrías que escribir tu propio `EmaStateStore`, o testearlo en Android con un `SavedStateHandle`
real y un test instrumentado. Un ViewModel que guarda la clave equivocada, o nunca restaura, pasa desapercibido hasta
que un usuario pierde lo que había escrito.

### `restoreStateOf(...)`

**Qué hace.** Crea la acción `EmaRestoreState` con los textos guardados, por clave, para compararla con
`assertActions`. Los valores son los textos tal como están en el store.

**Por qué importa.** La restauración es una acción que pasa por los middlewares y reducers. Con ella compruebas que
llega, y cuándo: justo después del inicializador, que se aplica primero.

**Qué pasa sin ella.** Tendrías que construir el `EmaRestoreState` a mano, o comprobarías solo el estado final, y el test
no podría distinguir si se ejecutó la restauración o si el inicializador ya dejó ese valor.

### Usarlos juntos

Los dos funcionan como un par: `testStateStore()` simula la muerte del proceso y `restoreStateOf(...)` comprueba lo que
recibe el nuevo ViewModel después.

```kotlin
@Test
fun `the form survives the death of the process`() = runTest {
    // 1. El primer ViewModel guarda en el store
    val store = testStateStore()
    val first = ProfileCreationViewModel(AddFriendUseCase(FakeFriendsRepository()), ProfileCreationState.DEFAULT, store)
    first.onCreated()
    first.dispatch(ProfileCreationAction.UserNameWritten("Ana"))

    assertEquals("""{"name":"Ana","surname":""}""", store.values["form"])    // lo que se guardó

    // 2. El proceso muere: un nuevo ViewModel recibe el mismo store
    val restored = ProfileCreationViewModel(AddFriendUseCase(FakeFriendsRepository()), ProfileCreationState.DEFAULT, store)
    val recorder = restored.record()
    restored.onCreated()

    // 3. La restauración llega justo después del inicializador, con los valores guardados
    recorder.assertActions(
        EmaInitializer.EMPTY,
        restoreStateOf("form" to """{"name":"Ana","surname":""}""")
    )
    assertEquals("Ana", restored.stateFlow.value.name)
}
```

## Cambiar piezas en los tests

Un ViewModel que [recibe sus piezas](dependency-injection.md#inyectar-piezas) en el constructor, como el side effect que
carga los amigos, se testea con la versión de esa pieza que necesite el test: una que responde al instante, una que
falla, una que tarda un rato.

- **No hace falta ninguna librería de mocks.** El mock es una clase normal que implementa la misma pieza, como
  `MockFriends` en [Inyección de dependencias](dependency-injection.md#inyectar-piezas). No lo configuras con
  `every { ... } returns` ni `when(...).thenReturn`: lo creas con los datos que quieres, `MockFriends(friends = null)`.
- **Solo es falsa la pieza que cambias.** Los reducers, los middlewares y los eventos del ViewModel son los reales, los
  mismos que en producción. El mock envía las mismas acciones que la pieza real, así que el test comprueba que la
  pantalla reacciona correctamente a ellas.

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
        HomeAction.RefreshRequested, HomeAction.SideEffects.QueryStarted, HomeAction.RefreshRequested, HomeAction.SideEffects.QueryLoaded(listOf(ana))
    )
}
```

### El tiempo es virtual

Muchas pantallas esperan a propósito: una búsqueda que espera 300 ms tras la última tecla (un debounce), una petición que
se rinde tras 10 segundos (un timeout), un estado de carga que dura lo que dura la petición. En un test no quieres
esperar por nada de eso.

**Qué significa.** `runTest` tiene un **reloj virtual**: `delay(3.seconds)` no hace que el test espere tres segundos. El
dispatcher del test anota que el trabajo debe reanudarse dentro de 3 segundos, y el reloj solo se mueve cuando el test lo
mueve. Así un test que cubre una petición de tres segundos tarda milisegundos.

**Qué lo hace funcionar.** Los side effects y middlewares se ejecutan en el dispatcher del test (mira
[Instalar la configuración de test](#instalar-la-configuración-de-test)), así que sus `delay` usan el reloj de
`runTest`. Si se ejecutaran en un dispatcher real, cada `delay` sería real.

**Cómo mueves el reloj:**

| Función | Qué hace |
|---------|----------|
| `advanceTimeBy(duration)` | Mueve el reloj hacia delante y ejecuta lo que vencía **antes** de ese momento. El trabajo que vence exactamente en ese momento todavía no se ejecuta. |
| `runCurrent()` | Ejecuta lo que vence en el momento actual, sin mover el reloj. |
| `advanceUntilIdle()` | Mueve el reloj lo necesario hasta que no queda trabajo. Es la que usas cuando solo quieres el resultado final. |

**Comprobar un debounce.** Toma un side effect para `QueryWritten` con concurrencia `Latest` que espera 300 ms antes de
buscar. Cada tecla nueva cancela la espera anterior y la empieza de nuevo, así que solo se busca la última consulta:

```kotlin
viewModel.dispatch(HomeAction.QueryWritten("an"))
advanceTimeBy(200.milliseconds)
recorder.assertNoEvents()          // 200 ms: sigue esperando, no se ha buscado nada

viewModel.dispatch(HomeAction.QueryWritten("ana"))   // cancela la espera de "an" y empieza una nueva
advanceTimeBy(200.milliseconds)
recorder.assertNoEvents()          // 400 ms desde "an", pero solo 200 ms desde "ana": sigue esperando

advanceUntilIdle()                 // pasan los 300 ms de "ana"
recorder.assertState { it.friends == listOf(ana) && it.query == "ana" }
```

Sin el reloj virtual este test necesitaría `Thread.sleep` reales, duraría un segundo y fallaría en una máquina lenta.

**Comprobar un estado de carga.** Lo mismo ocurre con un mock con latencia: `advanceTimeBy` detiene la petición a mitad
de camino, así que puedes comprobar el estado de carga de una petición lenta:

```kotlin
@Test
fun `a slow load keeps the loading state until it finishes`() = runTest {
    val viewModel = HomeViewModel(loadFriends = MockFriends(friends = listOf(ana), latency = 3.seconds))
    val recorder = viewModel.record()

    viewModel.dispatch(HomeAction.RefreshRequested)
    advanceTimeBy(2.seconds)                               // 2 s de 3 s: la petición sigue en marcha
    recorder.assertState { it.isLoading }

    advanceUntilIdle()                                     // pasa el último segundo
    recorder.assertState { it.friends == listOf(ana) }
}
```

**Comprobar un timeout.** Con `timeout = 10.seconds` y un mock que nunca responde, `advanceTimeBy(11.seconds)` basta para
comprobar que se ejecutó `onTimeout` y que el estado muestra el error, sin esperar diez segundos.

### Testear un broadcast entre ViewModels

Un [broadcast](../concepts/broadcasts.md) va de un ViewModel a otros, así que para testearlo creas **ambos** y comprueba
que lo que envía uno llega al otro. Por ejemplo, la pantalla de perfil crea un amigo y la lista de inicio debe
mostrarlo.

La clave es la configuración: el broadcast viaja por el `broadcaster` de la configuración, así que los ViewModels que se
hablan deben recibir **la misma**. Con `testConfiguration`, créala una vez y pásala a ambos:

```kotlin
@Test
fun `a friend created in the profile reaches the home list`() = runTest {
    val configuration = testConfiguration(UnconfinedTestDispatcher(testScheduler))
    val home = HomeViewModel(GetFriendsUseCase(FakeRepository()), HomeState(), configuration)   // escucha FriendCreated
    val profile = ProfileViewModel(ProfileState(friend = ana), configuration)                    // envía FriendCreated
    val homeRecorder = home.record()

    profile.dispatch(ProfileAction.SaveClicked)       // su side effect emite FriendCreated(ana)
    advanceUntilIdle()

    homeRecorder.assertActions(ProfileBroadcast.FriendCreated(ana))
    homeRecorder.assertState { it.friends == listOf(ana) }
}
```

- **Crea el receptor antes de enviar.** Un broadcast se entrega a los ViewModels que escuchan en ese momento, a menos que
  el emisor use `keepUntilReceived`.
- **Graba el receptor**, no el emisor: el test comprueba lo que llega y cómo reacciona el receptor.
- **Si los dos ViewModels recibieran configuraciones distintas,** cada uno tendría su propio broadcaster y la acción
  nunca llegaría. El test fallaría con una lista de acciones vacía, no con un error que diga por qué.
- Con `useTestConfiguration()` los ViewModels ya comparten la configuración de Emanate, así que no necesitas pasarla.
- Para comprobar que el receptor declaró `listen<FriendCreated>()` sin crear el emisor, usa
  [`assertSetup()`](#comprobar-el-setup).
