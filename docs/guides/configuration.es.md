# Configuración

Emanate tiene una configuración para toda la aplicación, que se establece una vez, cuando arranca, con `Emanate.init`:

```kotlin
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Emanate.init(EmaConfiguration.Android(debug = BuildConfig.DEBUG))
    }
}
```

- Llámalo **antes de crear cualquier ViewModel**: los ViewModels leen la configuración cuando se construyen.
- Solo se puede llamar **una vez**. Una segunda llamada lanza `IllegalStateException`.
- Si nunca se llama, se usa la `EmaConfiguration()` por defecto. Es lo habitual en los tests unitarios y en las
  plataformas que no son Android.

Registra la aplicación en `AndroidManifest.xml` (`android:name=".MyApplication"`). Un ViewModel o un `EmaSource` también
puede recibir su propia configuración, que se usa en lugar de la de la aplicación. Mira
[Configuración explícita](#configuración-explícita).

## Qué se puede configurar

Cada propiedad configura una cosa. Las que contienen un objeto con varios parámetros los listan en su propia columna,
antes de lo que configura la propiedad.

| Propiedad            | Por defecto `EmaConfiguration()` | Parámetros | Qué configura |
|----------------------|--------------------------------|------------|---------------|
| `dispatchers` | `EmaDispatchers()` | **`main`**: dónde se ejecutan los ViewModels y `EmaSource`, y por tanto sus reducers y side effects. Por defecto `Dispatchers.Main`.<br>**`io`**: dónde se ejecutan los [casos de uso](../utils/use-cases.md), para red, disco y trabajo bloqueante. Por defecto `Dispatchers.IO`.<br>**`default`**: trabajo intensivo de CPU, como procesar imágenes. Por defecto `Dispatchers.Default`. | Los dispatchers de corrutinas. Mira [Dispatchers](#dispatchers). |
| `errorPolicy` | `EmaErrorPolicy.Catch()` | **`Catch(onError)`**: la excepción se captura y la pantalla sigue funcionando. `onError` es opcional y recibe cada `EmaError`, para enviarlo a un reporter de crashes.<br>**`Throw`**: la excepción se lanza de nuevo y la app falla al instante. | Qué pasa cuando un middleware, reducer o side effect lanza una excepción. Mira [Errores](#errores). |
| `logger` | `EmaLogger.Println` | **`log(tag, message)`**: escribe una línea. El tag es el nombre del ViewModel.<br>**`prefix`**: texto añadido antes de cada tag, para filtrar los logs de Emanate, como `EMA_HomeViewModel`. Establécelo con `EmaLogger(prefix) { tag, message -> ... }`. | Adónde van los logs. `null` los desactiva. Mira [Logs](#logs). |
| `printer` | `EmaDataClassPrinter.ToString` | **`print(value)`**: devuelve el texto del estado o de la acción. Por defecto su `toString()`.<br>**`indent`** y **`wrappedLineWidth`**: solo en `EmaAndroidDataClassPrinter`. Los espacios de cada nivel de anidamiento (2) y la longitud a partir de la cual un `String` largo se divide en líneas (80). | Cómo se convierten en texto los estados y acciones en los logs. Mira [Imprimir el estado](#imprimir-el-estado). |
| `closeTimeout` | 5 segundos |  | Cuánto tiempo puede seguir ejecutándose el trabajo de un ViewModel cerrado antes de cancelarse. Mira [Cerrar un ViewModel](#cerrar-un-viewmodel). |
| `sideEffectListener` | `null` | **`onSuccess(reflection, output, duration)`**: terminó sin error. Recibe su `Reflection`, lo que lanzó (`output`) y cuánto tardó.<br>**`onError(error, duration)`**: lanzó una excepción. Recibe el `EmaError` y cuánto tardó.<br>**`onFinish(reflection, duration)`**: terminó, con éxito, con error o cancelado. | Recibe cada side effect que termina, para registrarlo o medirlo. Mira [Medir los side effects](#medir-los-side-effects). |
| `broadcaster` | `EmaActionBroadcaster.Shared` | **`Shared`**: lo usa todo el proceso, así que cada ViewModel recibe las acciones emitidas por los demás.<br>**`EmaActionBroadcaster()`**: uno nuevo aísla los ViewModels que lo usan, por ejemplo los de una librería. | Entrega las acciones emitidas entre ViewModels. Mira [Librerías y SDKs](#librerías-y-sdks). |

### Configuración para Android

`EmaConfiguration.Android(debug)` es una configuración predefinida en `emanate-android`
(`com.carmabs.emanate.android.configuration.Android`). Es la que hay que usar en una aplicación Android, con
`BuildConfig.DEBUG` como flag. Cambia estas propiedades respecto a la configuración por defecto. Cada columna indica el
valor que tiene la propiedad, para que puedas compararlos:

| Propiedad            | `EmaConfiguration()`           | `Android(debug = true)`          | `Android(debug = false)`         |
|----------------------|--------------------------------|----------------------------------|----------------------------------|
| `errorPolicy`        | `Catch()`                      | `Throw`                          | `Catch()`                        |
| `logger`             | `EmaLogger.Println`            | `EmaLogger.Logcat`               | `null`                           |
| `dispatchers.main`   | `Dispatchers.Main`             | `Dispatchers.Main.immediate`     | `Dispatchers.Main.immediate`     |
| `printer`            | `EmaDataClassPrinter.ToString` | `EmaAndroidDataClassPrinter()`   | `EmaAndroidDataClassPrinter()`   |

- **`errorPolicy`.** Con `Throw`, una excepción en un middleware, reducer o side effect cierra la app al instante, así
  que te enteras mientras desarrollas. Con `Catch()`, se captura y la pantalla sigue funcionando. Mira
  [Errores](#errores).
- **`logger`.** En las builds de debug cada acción y estado se escribe en logcat con `Log.d`. En las builds de release no
  se registra nada, así que el estado del usuario, que puede tener datos personales, nunca llega a logcat. Mira
  [Logs](#logs).
- **`dispatchers.main`.** `Dispatchers.Main.immediate` es lo que usa el `viewModelScope` de Android: el trabajo iniciado
  desde el hilo principal, como enviar una acción desde un clic, se ejecuta al instante en lugar de esperar al siguiente
  frame.
- **`printer`.** `EmaAndroidDataClassPrinter` imprime cada campo, con sangría. En las builds de release no hay `logger`,
  así que no se usa. Mira [Imprimir el estado](#imprimir-el-estado).

Solo `errorPolicy` y `logger` dependen del flag. `closeTimeout`, `sideEffectListener`, `broadcaster` y los demás
dispatchers mantienen los valores por defecto en todas.

`EmaConfiguration` es una data class, así que cambia solo lo que necesites con `copy`:

```kotlin
Emanate.init(
    EmaConfiguration.Android(debug = BuildConfig.DEBUG).copy(
        closeTimeout = 10.seconds
    )
)
```

## Dispatchers

`EmaDispatchers` agrupa los tres dispatchers de corrutinas que usa Emanate, uno para cada tipo de trabajo. Es la
propiedad `dispatchers` de la configuración, y cada uno se aplica en un sitio distinto:

| Dispatcher | Por defecto                                               | Dónde se aplica |
|------------|-----------------------------------------------------------|-----------------|
| `main`     | `Dispatchers.Main`                                        | El scope de cada `EmaViewModel` y `EmaSource`. Todo lo que hace el ViewModel se ejecuta ahí: sus middlewares, sus reducers, sus side effects y el envío de sus acciones. |
| `io`       | `Dispatchers.IO` | Los [casos de uso](../utils/use-cases.md): `EmaUseCase` y `EmaFlowUseCase` cambian a él para ejecutarse, así que el side effect que los llama no bloquea `main`. |
| `default`  | `Dispatchers.Default`                                     | El trabajo intensivo de CPU de `emanate-android`: las extensiones que crean bitmaps desde un `ByteArray` o un recurso drawable (`toBitmap`, `getBitmapFromResource`, `getBitmapCropFromResource`), y las corrutinas de `EmaJobService`. |

**`main`** es el que más cambiarás. Un side effect se ejecuta en `main`, así que puede leer el estado y enviar acciones
con seguridad, y el trabajo que bloquea se delega a un caso de uso, que cambia solo a `io`:

```kotlin
sideEffects {
    add<HomeAction.RefreshRequested> {
        // Se ejecuta en main
        dispatch(HomeAction.SideEffects.QueryStarted)
        // El caso de uso cambia a io, y vuelve a main con el resultado
        getFriendsUseCase(GetFriendsUseCase.Input(state.query))
            .onSuccess { friends -> dispatch(HomeAction.SideEffects.QueryLoaded(friends)) }
    }
}
```

En los tests, sustituye `main` por el dispatcher del test, mira [Testing](testing.md).

**`io`** lo lee el caso de uso cuando se crea, desde `Emanate.configuration`, no desde la configuración del ViewModel
que lo llama. Para usar otro, por ejemplo el `io` de una [librería](#librerías-y-sdks), pásalo al constructor del caso
de uso.

**`default`** se lee de `Emanate.configuration` cuando se ejecuta el trabajo. Puedes usarlo para tu propio trabajo
intensivo de CPU, así se sustituye junto con los demás en los tests:

```kotlin
suspend fun sortContacts(contacts: List<Contact>) =
    withContext(Emanate.configuration.dispatchers.default) { contacts.sortedBy { it.name } }
```

`EmaConfiguration.Android(debug)` solo cambia `main`, mira [Configuración para Android](#configuración-para-android).
Cambia cualquiera de ellos con `copy`, manteniendo los demás:

```kotlin
val android = EmaConfiguration.Android(debug = BuildConfig.DEBUG)
Emanate.init(android.copy(dispatchers = android.dispatchers.copy(io = myIoDispatcher)))
```

## Imprimir el estado

Los logs imprimen cada estado y acción con el `printer` de la configuración. Toma este estado:

```kotlin
data class HomeState(
    val query: String = "",
    val isLoading: Boolean = false,
    val friends: List<User> = emptyList()
) : EmaState
```

Con el printer por defecto, `EmaDataClassPrinter.ToString`, es su `toString()`, en una sola línea:

```
HomeState(query=An, isLoading=false, friends=[User(name=Ana, role=USER), User(name=Andrés, role=ADMIN)])
```

`EmaConfiguration.Android(debug)` usa `EmaAndroidDataClassPrinter`, que lee los campos y aplica sangría a los objetos,
colecciones y mapas anidados, así que un estado grande se lee de un vistazo:

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

Lee los campos con reflexión de Java, así que solo está disponible en Android y la JVM. Cambia su sangría o el ancho a
partir del cual se dividen los textos largos con `EmaAndroidDataClassPrinter(indent = 4, wrappedLineWidth = 120)`.

### Un printer propio

Implementa `EmaDataClassPrinter` para imprimir los estados a tu manera. Un caso útil es **ocultar datos sensibles**, para
que no lleguen a los logs de una build de QA:

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

Para imprimir cualquier objeto con el printer de la configuración, usa `toStringPretty()`.

## Logs

Emanate escribe sus logs con el `logger` de la configuración. Un logger es una función que recibe un `tag`, el nombre del
ViewModel, y un `message`:

| Logger                         | Dónde escribe                                             | Lo usa                               |
|--------------------------------|-----------------------------------------------------------|--------------------------------------|
| `EmaLogger.Println`            | La salida estándar, con `EMA_` antes del tag.             | `EmaConfiguration()`                 |
| `EmaLogger.Logcat`             | Logcat con `Log.d`, con `EMA_` antes del tag.             | `EmaConfiguration.Android(true)`     |
| `null`                         | En ningún sitio.                                          | `EmaConfiguration.Android(false)`    |
| Tu propio `EmaLogger`          | Donde quieras.                                            | Cualquiera                           |

Cada `EmaViewModel` registra sus acciones con su nombre como tag:

- Una línea cuando se recibe una acción, antes de los middlewares. Muestra las acciones que detiene un middleware, que
  no llegan a los reducers.
- Un bloque con la acción y el estado antes y después de sus reducers. Se escribe justo antes de ellos, después de todos
  los middlewares, así que el estado posterior a la acción es correcto aunque un middleware la continúe más tarde con
  `nextAfter`, y la acción es la que reciben los reducers.

Para la acción `QueryWritten("An")` del estado anterior, logcat muestra (se omite el tag, `EMA_HomeViewModel`):

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

Los errores capturados por la [política de errores](#errores) también se registran, con el mismo tag.

El estado puede contener datos del usuario, así que no registres logs en las builds de release, como hace
`EmaConfiguration.Android(debug)`.

### Un logger propio

Un `EmaLogger` es una lambda, así que puede enviar los logs adonde necesites. Para usar Timber en las builds de debug:

```kotlin
Emanate.init(
    EmaConfiguration.Android(debug = BuildConfig.DEBUG).copy(
        logger = if (BuildConfig.DEBUG) EmaLogger { tag, message -> Timber.tag(tag).d(message) } else null
    )
)
```

O guarda las últimas acciones como breadcrumbs de un reporte de crash, y omite los bloques grandes de estado:

```kotlin
val breadcrumbs = EmaLogger { tag, message ->
    // "Received action: ..." es una línea y basta para saber qué hizo el usuario antes del crash
    if (message.startsWith("Received action")) FirebaseCrashlytics.getInstance().log("$tag: $message")
}
```

Usa `EmaLogger(prefix = "MYAPP_") { tag, message -> ... }` para añadir un prefijo a cada tag, de modo que se puedan
filtrar en logcat.

Para desactivar los logs de un solo ViewModel, pásale una configuración sin logger:
`Emanate.configuration.copy(logger = null)`. Un `EmaSource` usado por sí solo no registra logs: añade
`loggerMiddleware(tag, logger)` como su último middleware.

## Errores

El código de tu ViewModel puede fallar: un reducer que llama a `friends.first()` en una lista vacía, un middleware que
parsea un valor erróneo, un side effect que lanza una excepción. Lo que hace Emanate entonces es la `errorPolicy`.

> **Los errores que esperas no son excepciones.** Una petición que puede fallar no es un error de este tipo: devuelve un
> [`EmaResult`](../utils/results.md) desde el caso de uso y deja que el side effect envíe una acción que muestre el
> fallo. La política es para los **bugs**, lo que no esperabas.

Toma un reducer con un bug:

```kotlin
reducers {
    add<HomeAction.FriendClicked> { action ->
        // Bug: lanza NoSuchElementException cuando el amigo no está en la lista
        state.copy(selected = state.friends.first { it.id == action.id })
    }
}
```

| | `Throw` | `Catch` |
|---|---------|---------|
| **Qué pasa** | La excepción se lanza de nuevo donde ocurrió, así que la app se cierra con su stack trace. | La excepción se captura. La app sigue funcionando. |
| **La pantalla** | Cerrada por el crash. | Sigue funcionando. El estado es el que tenía antes de la acción. |
| **El log** | El crash de la app. | Una línea con el stack trace: `HomeViewModel: action FriendClicked failed: java.util.NoSuchElementException ...` |
| **`onError`** | No se llama. | Se llama con un `EmaError`. |
| **Úsalo** | Mientras desarrollas, para que el bug no pase desapercibido. Es el valor por defecto de `EmaConfiguration.Android(true)`. | En producción, para que un bug en una acción no tire abajo toda la app. Es el valor por defecto de `EmaConfiguration()` y de las builds de release. |

Qué significa "sigue funcionando" depende de la pieza que falló:

| Lanzó en...     | Con `Catch`                                                                                   |
|-----------------|-----------------------------------------------------------------------------------------------|
| Un reducer      | La acción se descarta: el estado no cambia y las siguientes acciones se procesan como siempre. |
| Un middleware   | La acción se descarta y no llega a los reducers ni a los side effects.                        |
| Un side effect  | Ese side effect termina. El resto de side effects y las acciones siguen funcionando.          |

Las cancelaciones nunca son errores: un side effect cancelado por su concurrencia, su timeout o el cierre del ViewModel
no se reporta.

### Reportar los errores

Con `Catch`, pasa `onError` para enviar cada error a tu reporter de crashes, de modo que un bug silencioso para el
usuario no sea silencioso para ti:

```kotlin
Emanate.init(
    EmaConfiguration.Android(debug = BuildConfig.DEBUG).copy(
        errorPolicy = if (BuildConfig.DEBUG) {
            EmaErrorPolicy.Throw
        } else {
            EmaErrorPolicy.Catch { error ->
                crashReporter.recordException(error.exception)
                crashReporter.setCustomKey("viewModel", error.reflection.containerClassName)
                crashReporter.setCustomKey("where", error.reflection.name)
            }
        }
    )
)
```

Un `EmaError` tiene:

| Campo                                     | Ejemplo                                          |
|-------------------------------------------|--------------------------------------------------|
| `exception`                               | La `NoSuchElementException` que se lanzó.        |
| `reflection.containerClassName`           | `HomeViewModel`                                  |
| `reflection.containerClassQualifiedName`  | `com.carmabs.sample.home.HomeViewModel`          |
| `reflection.name`                   | `action FriendClicked` para un middleware o un reducer. Para un side effect, su nombre: `RefreshRequested`, mira [Nombrar los side effects](#nombrar-los-side-effects). |

Para que también falle en las builds de QA, de modo que los testers reporten los bugs en lugar de que la app los
esconda, usa `Throw` ahí: el flag `debug` es solo el valor por defecto de `EmaConfiguration.Android`, y la política la
eliges tú.

## Medir los side effects

Para registrar los side effects, por ejemplo para medir cuánto tardan, contar cuántos fallan o enviarlos a tus
analíticas, establece un `sideEffectListener`. Se llama para cada side effect de cada ViewModel, y tiene tres funciones,
todas opcionales. Cada una recibe la `duration` que tardó el side effect:

| Función                           | Se llama cuando                                              | Recibe                                         |
|-----------------------------------|--------------------------------------------------------------|------------------------------------------------|
| `onSuccess(reflection, output, duration)` | Un side effect termina sin error.                    | La `reflection` y su `output`: lo que lanzó mientras se ejecutaba. |
| `onError(error, duration)`        | Un side effect lanza una excepción, con `Catch` y con `Throw` (justo antes de que falle la app). | Un `EmaError`: la `reflection` y la `exception`. |
| `onFinish(reflection, duration)`  | Un side effect termina: después de `onSuccess`, después de `onError`, o cuando se cancela. | La `reflection`.             |

Una cancelación (una acción nueva con `Latest`, un timeout, el cierre del ViewModel) no es un error, así que solo llama
a `onFinish`. Así distingues los tres finales: `onSuccess` es un éxito, `onError` un fallo, y un `onFinish` sin ninguno
de los dos antes es una cancelación.

La `Reflection` dice de qué side effect se trata:

| Campo                         | Ejemplo                                  |
|-------------------------------|------------------------------------------|
| `containerClassName`          | `HomeViewModel`                          |
| `containerClassQualifiedName` | `com.carmabs.sample.home.HomeViewModel`  |
| `name`                        | El nombre del side effect, mira [Nombrar los side effects](#nombrar-los-side-effects). |

El `SideEffectOutput` dice lo que produjo el side effect, en el orden en que ocurrió:

| Campo        | Qué tiene                                                                                       |
|--------------|-------------------------------------------------------------------------------------------------|
| `actions`    | Las acciones que envió a su ViewModel con `dispatch`: `Started`, `Loaded(friends)`...           |
| `broadcasts` | Las acciones que envió a otros ViewModels con `broadcast`.                                      |
| `events`     | Los eventos que publicó para la vista con `publish`.                                             |

Solo tiene lo que lanzó **esa ejecución**, no lo que lanzaron otros side effects al mismo tiempo. Con todo ello puedes
registrar, de cada side effect, qué ViewModel lo lanzó, su nombre, cuánto tardó, cómo terminó y qué produjo:

```kotlin
class SideEffectTracker(private val analytics: Analytics) : EmaSideEffectListener {

    override fun onSuccess(reflection: Reflection, output: SideEffectOutput, duration: Duration) {
        analytics.track(
            event = "side_effect_success",
            "name" to "${reflection.containerClassName}.${reflection.name}",
            "millis" to duration.inWholeMilliseconds.toString(),
            // Loaded(friends = [...]), Failed...
            "actions" to output.actions.joinToString { it::class.simpleName.toString() }
        )
    }

    override fun onError(error: EmaError, duration: Duration) {
        analytics.track(
            event = "side_effect_error",
            "viewModel" to error.reflection.containerClassName,
            "name" to error.reflection.name,
            "error" to error.exception::class.simpleName.toString(),
            "millis" to duration.inWholeMilliseconds.toString()
        )
    }

    override fun onFinish(reflection: Reflection, duration: Duration) {
        // También lo llaman los cancelados
    }
}

Emanate.init(
    EmaConfiguration.Android(debug = BuildConfig.DEBUG).copy(sideEffectListener = SideEffectTracker(analytics))
)
```

- **`onSuccess` y `onError` sirven para registrar, no para gestionar.** El error también lo gestiona la
  [política de errores](#errores): el `onError` del listener te informa de **ese side effect y su tiempo**, mientras que
  el `onError` de `EmaErrorPolicy.Catch` es el sitio donde reportar bugs.
- **Un side effect no devuelve un valor**: comunica con acciones y eventos, así que eso es lo que tiene `output`. El
  `output` de los bloques `onClose` también tiene lo que lanzaron. Ten en cuenta que las acciones pueden llevar datos del
  usuario, como los `friends` de `Loaded(friends)`: envía su tipo, como en el ejemplo, si esos datos no deben salir de la
  app.
- **La duración** es el tiempo que estuvo en ejecución el side effect, incluidos los `delay` y las suspensiones. Un side
  effect que termina por su `timeout` llama a `onSuccess` tras el tiempo del timeout.

## Nombrar los side effects

El `name` de la `Reflection` identifica el trabajo en los errores y en el `sideEffectListener`. Emanate le da un nombre a
partir de lo que sabe:

| Trabajo                                | Nombre                              | Ejemplo              |
|----------------------------------------|-------------------------------------|----------------------|
| Un side effect del `setup`             | El tipo de la acción que lo lanza, o el `name` que pases. | `RefreshRequested` |
| Un `nextAfter` de un middleware        | `nextAfter` y el tipo de la acción que recibió. | `nextAfter QueryWritten` |
| Un bloque `onClose`                    | `onClose`                           | `onClose`            |
| Un reducer o middleware que falla (solo errores) | `action` y el tipo de la acción. | `action FriendClicked` |

```kotlin
sideEffects {
    add<HomeAction.RefreshRequested> { /* ... */ }                       // RefreshRequested
    add<HomeAction.FriendClicked>(name = "openProfile") { /* ... */ }    // openProfile
}
```

Pasa `name` a `add`, a `sideEffectOf` o a la propiedad `name` de una clase que implemente `SideEffect`, cuando el tipo de
la acción no sea un buen nombre, por ejemplo cuando dos side effects escuchan la misma acción o un tipo base como
`EmaAction`.

## Cerrar un ViewModel

Cuando se cierra un ViewModel, sus side effects se cancelan, así que el trabajo que estaba en marcha se detiene. Dos
cosas todavía pueden ejecutarse después:

- los bloques `onClose` de su `EmaSource`, que son lo último que hace el ViewModel, y
- los side effects añadidos con `survivesClose = true`, como el que guarda un borrador cuando el usuario sale del editor.

El `closeTimeout` es el tiempo del que disponen. Pasado ese tiempo, se cancelan, para que una petición que nunca responde
no mantenga vivo el ViewModel para siempre. Por defecto son 5 segundos.

```kotlin
sideEffects {
    add<EditorAction.SaveClicked>(survivesClose = true) { saveDraftUseCase(state.draft) }
}
```

Si el usuario sale del editor y el guardado tarda 8 segundos, se cancela en el quinto. Para un trabajo más lento,
auméntalo:

```kotlin
Emanate.init(EmaConfiguration.Android(debug = BuildConfig.DEBUG).copy(closeTimeout = 15.seconds))
```

Mantenlo corto: es el tiempo que permanece en memoria un ViewModel que ya estaba cerrado. Mira
[Side effects](../concepts/side-effects.md#cuando-se-cierra-la-pantalla).

## Configuración explícita

`Emanate.configuration` es compartida por todo el proceso. `EmaViewModel` y `EmaSource(...)` la leen cuando se crean,
como valor por defecto de su parámetro `configuration`, que va justo después del estado inicial. Pasa una para usarla en
su lugar:

```kotlin
class HomeViewModel(
    private val getFriendsUseCase: GetFriendsUseCase,
    configuration: EmaConfiguration = Emanate.configuration
) : EmaViewModel<HomeState, HomeAction, HomeEvent>(HomeState(), configuration) {
    // ...
}
```

El ViewModel se ejecuta en su dispatcher `main` y registra logs con su `logger`. La configuración la conservan el
ViewModel y su `EmaSource`, así que su política de errores, su listener de side effects, `closeTimeout` y broadcaster
son los de la configuración que recibieron.

**Usa `Emanate.init`** en una aplicación: configura todos los ViewModels en un único sitio, y no necesitan ningún
parámetro. **Pasa la configuración** cuando la de la aplicación no basta o no se puede dar por hecha:

- **Librerías y SDKs.** `Emanate.init` solo se puede llamar una vez y pertenece a la aplicación. Una librería construida
  con Emanate pasa su propia configuración a sus ViewModels, para no chocar con una aplicación que también use Emanate,
  ni depender de si se inicializó. Mira [Librerías y SDKs](#librerías-y-sdks).
- **Un módulo con una configuración distinta**, por ejemplo su propio logger o política de errores.
- **Inyección de dependencias.** La configuración es una dependencia más, proporcionada por Koin o cualquier otro
  framework de inyección de dependencias. Mira
  [Inyección de dependencias](dependency-injection.md#inyectar-la-configuración).
- **Tests.** Cada test pasa su configuración, con los dispatchers del test, sin cambiar la de Emanate, así que los tests
  pueden ejecutarse en paralelo. Mira [Testing](testing.md#sin-cambiar-la-configuración-de-emanate).

Los [casos de uso](../utils/use-cases.md) no reciben toda la configuración, solo un dispatcher en el constructor: por
defecto el dispatcher `io` de la configuración de Emanate, así que pasa el dispatcher `io` de la tuya.

## Librerías y SDKs

Una librería construida con Emanate puede usarla una aplicación que también use Emanate y llame a `Emanate.init`. Para
mantenerlas separadas, la librería no llama a `Emanate.init` ni usa la configuración de Emanate: crea la suya y se la
pasa a todas sus piezas.

```kotlin
internal object MyLibraryEmanate {
    val configuration = EmaConfiguration.Android(debug = BuildConfig.DEBUG).copy(
        logger = if (BuildConfig.DEBUG) EmaLogger(prefix = "MYLIB_") { tag, message -> Log.d(tag, message) } else null,
        broadcaster = EmaActionBroadcaster()
    )
}
```

### Envuelve las clases de Emanate en clases base

Pasar la configuración en cada ViewModel y caso de uso de la librería es fácil de olvidar: el que no la recibe usa en
silencio la de la aplicación, o la de por defecto si la aplicación nunca inicializó Emanate. **No extiendas
directamente las clases de Emanate en la librería: crea una clase base para cada una que ya pase la configuración**, y
extiende solo esas:

```kotlin
internal abstract class MyLibViewModel<S : EmaState, A : EmaAction, E : EmaEvent>(initialState: S) :
    EmaViewModel<S, A, E>(initialState, MyLibraryEmanate.configuration)

internal abstract class MyLibUseCase<I, O> :
    EmaUseCase<I, O>(MyLibraryEmanate.configuration.dispatchers.io)

internal abstract class MyLibFlowUseCase<I, O> :
    EmaFlowUseCase<I, O>(MyLibraryEmanate.configuration.dispatchers.io)
```

Las clases de la librería las extienden, y no hay nada que recordar:

```kotlin
internal class PayUseCase(private val repository: PaymentRepository) : MyLibUseCase<Payment, Receipt>() {
    // ...
}

internal class PaymentViewModel(private val payUseCase: PayUseCase) :
    MyLibViewModel<PaymentState, PaymentAction, PaymentEvent>(PaymentState()) {
    // ...
}
```

Las clases base también son el sitio donde añadir lo que comparten todas las piezas de la librería, como un plugin que
instalan todos sus ViewModels. Si la librería crea un `EmaSource` por su cuenta con `EmaSource(...)`, envuélvelo de la
misma forma.

- **Acciones de broadcast.** Con su propio `EmaActionBroadcaster`, los ViewModels de la librería solo reciben las
  acciones emitidas entre ellos, y los de la aplicación no reciben las suyas. Mantén el compartido,
  `EmaActionBroadcaster.Shared`, si la librería y la aplicación se envían resultados entre sí.
- **ViewModels públicos.** Si la aplicación debe crear los ViewModels de la librería, expón factorías o una clase base
  pública propia, no las clases de Emanate, para que no se pueda omitir la configuración.

Si la aplicación debe poder cambiar algo, como el logger, añade una función a la librería que lo reciba antes de abrir
cualquiera de sus pantallas, en lugar de usar `Emanate.init`.

La aplicación y la librería usan el mismo `emanate-core`: la versión que resuelve Gradle para ambas. Mantén la librería
en una versión compatible con las aplicaciones que la usan.
