# Configuración

Emanate tiene una configuración para toda la aplicación, que se fija una vez, cuando arranca, con `Emanate.init`:

```kotlin
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Emanate.init(EmaConfiguration.Android(debug = BuildConfig.DEBUG))
    }
}
```

- Llámalo **antes de crear ningún ViewModel**: los ViewModels leen la configuración al construirse.
- Solo se puede llamar **una vez**. Una segunda llamada lanza `IllegalStateException`.
- Si no se llama nunca, se usa la configuración por defecto, `EmaConfiguration()`. Es lo habitual en los tests unitarios
  y en las plataformas que no son Android.

Un ViewModel o una source también pueden recibir su propia configuración, que se usa en lugar de la de la aplicación.
Consulta [Configuración explícita](#configuración-explícita).

## Qué se puede configurar

Cada propiedad configura una cosa:

| Propiedad            | `EmaConfiguration()` por defecto | `EmaConfiguration.Android(debug)`             | Para qué |
|----------------------|----------------------------------|-----------------------------------------------|----------|
| `dispatchers`        | `EmaDispatchers()`               | `main` es `Dispatchers.Main.immediate`        | Dónde se ejecutan los ViewModels, los casos de uso y el trabajo de cálculo. Consulta [Dispatchers](#dispatchers). |
| `errorPolicy`        | `EmaErrorPolicy.Catch()`         | `Throw` en debug, `Catch()` en release        | Qué pasa cuando un middleware, reducer o side effect lanza una excepción. Consulta [Errores](async-and-errors.es.md#errores). |
| `logger`             | `EmaLogger.Println`              | `EmaLogger.Logcat` en debug, `null` en release | Dónde van los logs de Emanate. Consulta [Logs](#logs). |
| `printer`            | `EmaDataClassPrinter.ToString`   | Imprime todos los campos, con sangría         | Cómo se imprimen los objetos en los logs y con `toStringPretty()`. |
| `destroyedTimeout`   | 5 segundos                       | 5 segundos                                    | Cuánto pueden ejecutarse los side effects de `Lifecycle.Destroyed` después de destruir el ViewModel, salvo que tengan su propio `timeout`. |
| `sideEffectListener` | `null`                           | `null`                                        | Recibe el resultado de cada side effect, para seguirlos o medirlos. |
| `methodNameResolver` | Nombres genéricos                | Se obtienen de las lambdas                    | El nombre de la función que lanzó un side effect, que aparece en los errores. Consulta [Nombrar los side effects](#nombrar-los-side-effects). |
| `broadcaster`        | `EmaActionBroadcaster.Shared`    | `EmaActionBroadcaster.Shared`                 | Envía las acciones que se emiten entre ViewModels. Consulta [Librerías y SDKs](#librerías-y-sdks). |

`EmaConfiguration.Android(debug)` está en `emanate-android` (`com.carmabs.emanate.android.configuration.Android`). Pasa
`BuildConfig.DEBUG`: las builds de debug registran en logcat y fallan en el momento con cualquier excepción, y las de
release no registran nada, así que el estado no llega a logcat, y siguen funcionando después de una excepción.

`EmaConfiguration` es una data class, así que cambia solo lo que necesites con `copy`:

```kotlin
Emanate.init(
    EmaConfiguration.Android(debug = BuildConfig.DEBUG).copy(
        errorPolicy = EmaErrorPolicy.Catch { error -> crashReporter.record(error.exception) }
    )
)
```

## Dispatchers

`EmaDispatchers` agrupa los dispatchers que usa Emanate por defecto:

| Dispatcher | Por defecto                                            | Lo usan                                                                            |
|------------|--------------------------------------------------------|------------------------------------------------------------------------------------|
| `main`     | `Dispatchers.Main`                                     | Los ViewModels y las sources, por defecto.                                         |
| `io`       | `Dispatchers.IO`, o `Dispatchers.Default` en JS y Wasm | Los [casos de uso](utilities.es.md#casos-de-uso): red, disco y trabajo bloqueante. |
| `default`  | `Dispatchers.Default`                                  | El trabajo de cálculo, como procesar imágenes en `emanate-android`.                |

`EmaConfiguration.Android(debug)` usa `Dispatchers.Main.immediate` como `main`, igual que el `viewModelScope` de Android: el
trabajo que empieza en el hilo principal, como despachar una acción desde un clic, se ejecuta en el momento en lugar de
esperar al siguiente frame.

Cambia cualquiera de ellos con `copy`, manteniendo los demás:

```kotlin
val android = EmaConfiguration.Android(debug = BuildConfig.DEBUG)
Emanate.init(android.copy(dispatchers = android.dispatchers.copy(io = myIoDispatcher)))
```

## Nombrar los side effects

Cada `sideEffect` informa del nombre de la función que lo lanzó, algo útil para registrar los errores.

- `EmaConfiguration.Android(debug)` lo obtiene de la clase que el compilador genera para la lambda, así que un `sideEffect` en
  `doLogin()` aparece como `doLogin`.
- En otras plataformas el nombre es genérico. Pasa `logName` para ponerlo tú, en cualquier plataforma:

```kotlin
sideEffect(logName = "login") { loginUseCase(input) }
```

Para decidir el nombre de otra forma, implementa `EmaMethodNameResolver` y asígnalo en `EmaConfiguration.methodNameResolver`.

## Imprimir objetos

`toStringPretty()` imprime cualquier objeto con el `printer` de la configuración. `EmaConfiguration.Android(debug)` usa
`EmaAndroidDataClassPrinter`, que imprime los campos, los objetos anidados, las colecciones y los mapas con sangría.
Implementa `EmaDataClassPrinter` para imprimirlos a tu manera.

## Logs

Emanate escribe sus logs con el `logger` de la configuración. Por defecto es `EmaLogger.Println`, que escribe en la
salida estándar y añade `EMA_` delante del tag. `EmaConfiguration.Android(debug)` usa `EmaLogger.Logcat` en las builds
de debug, que escribe con `Log.d`, y ningún logger en las de release. Los errores que captura la
[política de errores](async-and-errors.es.md#errores) también se registran.

Cada `EmaViewModel` lo usa, con su nombre como tag, para registrar sus acciones:

- Una línea cuando recibe una acción, antes de los middlewares: `Received action: Search(query=ema)`. Muestra las
  acciones que detiene un middleware, que no llegan a los reducers.
- Un bloque con la acción y el estado antes y después de sus reducers. Se escribe justo antes de ellos, después de todos
  los middlewares, así que el estado después de la acción es correcto aunque un middleware la continúe más tarde con
  `nextAfter`, y la acción es la que reciben los reducers.

El estado puede contener datos del usuario, así que no registres nada en las builds de release, como hace
`EmaConfiguration.Android(debug)`. Para enviar los logs a tu propio logger:

```kotlin
Emanate.init(
    EmaConfiguration.Android(debug = BuildConfig.DEBUG).copy(
        logger = if (BuildConfig.DEBUG) EmaLogger { tag, message -> Timber.tag(tag).d(message) } else null
    )
)
```

Un ViewModel puede pasar su propio `logger` en el constructor, o `null` para desactivar solo sus logs. Una source
usada por sí sola no registra nada: añade `loggerMiddleware(tag, logger)` como su último middleware.

## Configuración explícita

`Emanate.configuration` es compartida por todo el proceso. `EmaViewModel`, `EmaViewModelClassic`, `EmaViewModelAction`
y `EmaSource(...)` la leen al crearse, como valor por defecto de su parámetro `configuration`, que va justo después del
estado inicial. Pásales una para que la usen en su lugar:

```kotlin
class LoginViewModel(
    private val loginUseCase: LoginUseCase,
    configuration: EmaConfiguration = Emanate.configuration
) : EmaViewModel<LoginState, LoginAction, LoginEvent>(LoginState.DEFAULT, configuration) {
    // ...
}
```

Los parámetros `dispatcher` y `logger` toman de ella su valor por defecto, y se pueden seguir pasando por nombre para
cambiar solo esos. El ViewModel y su source guardan la configuración, así que su política de errores, su listener de
side effects, su `destroyedTimeout` y su broadcaster son los de la configuración que recibieron.

**Usa `Emanate.init`** en una aplicación: configura todos los ViewModels en un solo sitio y no necesitan ningún
parámetro. **Pasa la configuración** cuando la de la aplicación no basta o no se puede contar con ella:

- **Librerías y SDKs.** `Emanate.init` solo se puede llamar una vez y le corresponde a la aplicación. Una librería hecha
  con Emanate pasa su propia configuración a sus ViewModels, así no choca con una aplicación que también use Emanate ni
  depende de que se haya inicializado. Consulta [Librerías y SDKs](#librerías-y-sdks).
- **Un módulo con otra configuración**, por ejemplo con su propio logger o su propia política de errores.
- **Inyección de dependencias.** La configuración es una dependencia más, que proporciona Koin u otro framework de inyección de dependencias. Consulta
  [Inyección de dependencias](dependency-injection.es.md#inyectar-la-configuración).
- **Tests.** Cada test pasa su configuración, con los dispatchers del test, sin cambiar la de Emanate, así que los tests
  se pueden ejecutar en paralelo. Consulta [Testing](testing.es.md#sin-cambiar-la-configuración-de-emanate).

Los [casos de uso](utilities.es.md#casos-de-uso) no reciben la configuración entera, solo un dispatcher en el
constructor: por defecto el dispatcher `io` de la configuración de Emanate, así que pásales el `io` de la tuya.
`toStringPretty()` imprime con el `printer` de la configuración de Emanate salvo que reciba otro.

## Librerías y SDKs

Una librería hecha con Emanate puede usarla una aplicación que también usa Emanate y llama a `Emanate.init`. Para
mantenerlas separadas, la librería no llama a `Emanate.init` ni usa la configuración de Emanate: crea la suya y se la
pasa a todas sus piezas.

```kotlin
internal object MyLibraryEmanate {
    val configuration = EmaConfiguration.Android(debug = BuildConfig.DEBUG).copy(
        logger = if (BuildConfig.DEBUG) EmaLogger(prefix = "MYLIB_") { tag, message -> Log.d(tag, message) } else null,
        broadcaster = EmaActionBroadcaster()
    )
}

internal class PayUseCase(private val repository: PaymentRepository) :
    EmaUseCase<Payment, Receipt>(MyLibraryEmanate.configuration.dispatchers.io) {
    // ...
}

internal class PaymentViewModel(private val payUseCase: PayUseCase) :
    EmaViewModel<PaymentState, PaymentAction, PaymentEvent>(PaymentState(), MyLibraryEmanate.configuration) {
    // ...
}
```

- **Los ViewModels y las sources** reciben la configuración de la librería en su parámetro `configuration`.
- **Los casos de uso** reciben su dispatcher `io`.
- **Para imprimir objetos:** `toStringPretty(MyLibraryEmanate.configuration.printer)`.
- **Las acciones emitidas.** Con su propio `EmaActionBroadcaster`, los ViewModels de la librería solo reciben las
  acciones que se emiten entre ellos, y los de la aplicación no reciben las suyas. Deja el compartido,
  `EmaActionBroadcaster.Shared`, si la librería y la aplicación se envían resultados.

Si la aplicación tiene que poder cambiar algo, como el logger, añade a la librería una función que lo reciba antes de
abrir ninguna de sus pantallas, en lugar de usar `Emanate.init`.

La aplicación y la librería usan el mismo `emanate-core`: la versión que Gradle resuelve para las dos. Mantén la
librería en una versión compatible con las aplicaciones que la usan.
