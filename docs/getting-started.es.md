# Primeros pasos

## Requisitos

- Android `minSdk` 24 o superior.
- La librería se compila con Java 21 y Kotlin 2.

## Instalación

Añade el repositorio de JitPack:

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

Añade las dependencias:

```kotlin
val emanateVersion = "7.0.0"

dependencies {
    implementation("com.github.carmabs.emanate:emanate-android:$emanateVersion")
    implementation("com.github.carmabs.emanate:emanate-compose-android:$emanateVersion")
    // Solo si tus pantallas están en un grafo de Navigation Compose
    implementation("com.github.carmabs.emanate:emanate-compose-android-nav2:$emanateVersion")
}
```

Para un módulo Kotlin puro, como un módulo `presentation` con los ViewModels, añade solo el core, y `emanate-test` para
testearlos:

```kotlin
dependencies {
    implementation("com.github.carmabs.emanate:emanate-core:$emanateVersion")
    testImplementation("com.github.carmabs.emanate:emanate-test:$emanateVersion")
}
```

`emanate-core` es una librería Kotlin Multiplatform, así que también se puede usar desde el `commonMain` de un módulo
multiplataforma.

## Módulos

| Artefacto                      | Úsalo para                                                                                                                                         |
| ------------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------- |
| `emanate-core`                 | Estado, acciones, eventos, ViewModels, `EmaSource`, plugins y casos de uso. Kotlin Multiplatform puro.                                             |
| `emanate-compose-android`      | Pantallas con Jetpack Compose, botón atrás, diálogos, permisos y previews. No depende de ninguna librería de navegación.                           |
| `emanate-compose-android-nav2` | Pantallas de Emanate en un grafo de Navigation Compose (`emaScreen`, `emaDialog`). Mira [Navegación en Compose](compose/navigation.md).            |
| `emanate-android`              | La integración con la plataforma Android: configuración, inicializadores, permisos y el almacén del estado guardado.                               |
| `emanate-serialization`        | Guarda cualquier valor serializable del estado ante la muerte del proceso. Basado en kotlinx.serialization. Mira el [plugin de backup](guides/process-death.md). |
| `emanate-test`                 | Registra y comprueba `EmaViewModel` y `EmaSource` en tus tests unitarios. Mira [Testing](guides/testing.md).                                       |

## Inicializa Emanate

Llama a `Emanate.init` una sola vez, cuando arranca la aplicación y antes de crear cualquier ViewModel:

```kotlin
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Emanate.init(EmaConfiguration.Android(debug = BuildConfig.DEBUG))
    }
}
```

Registra la aplicación en `AndroidManifest.xml` (`android:name=".MyApplication"`). Con `BuildConfig.DEBUG`, las builds de
debug registran cada acción y fallan al instante con cualquier excepción, y las de release no registran nada y siguen
funcionando. Mira [Configuración](guides/configuration.md).

## Tu primera pantalla

Una pantalla se compone de un **estado**, las **acciones** que puede realizar el usuario, los **eventos** que puede
emitir y un **ViewModel** que los conecta. Esto es un contador.

```kotlin
data class CounterState(val count: Int) : EmaState {

    companion object {
        val DEFAULT = CounterState(count = 0)
    }
}

sealed interface CounterAction : EmaAction {
    data object Increment : CounterAction
}

sealed interface CounterEvent : EmaEvent {
    data class LimitReached(val limit: Int) : CounterEvent
}

class CounterViewModel :
    EmaViewModel<CounterState, CounterAction, CounterEvent>(CounterState.DEFAULT) {

    override fun setup() = source {
        reducers {
            add<CounterAction> { action ->
                when (action) {
                    is CounterAction.Increment -> state.copy(count = state.count + 1)
                }
            }
        }
        sideEffects {
            add<CounterAction.Increment> {
                if (state.count == LIMIT) publish(CounterEvent.LimitReached(LIMIT))
            }
        }
    }

    private companion object {
        const val LIMIT = 10
    }
}
```

- El **reducer** devuelve el siguiente estado para cada acción, y la vista se entera.
- El **side effect** se ejecuta después del reducer, con el nuevo estado, y `publish` envía un evento de un solo uso a la
  vista.
- El ViewModel nunca toca clases de la plataforma, así que se puede testear como Kotlin puro y compartir entre
  plataformas.

### La pantalla

Una pantalla Compose se describe con un `EmaScreenContent`:

```kotlin
class CounterScreenContent : EmaScreenContent<CounterState, CounterAction> {

    @Composable
    override fun onState(
        state: CounterState,
        actions: EmaActionDispatcher<CounterAction>
    ) {
        Column {
            Text(text = state.count.toString())
            Button(onClick = { actions.dispatch(CounterAction.Increment) }) {
                Text(text = "+")
            }
        }
    }
}
```

Y se añade al grafo de navegación de una activity, donde se gestionan sus eventos:

```kotlin
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            NavHost(
                navController = navController,
                startDestination = CounterScreenContent::class.routeId
            ) {
                emaScreen(
                    screenContent = CounterScreenContent(),
                    viewModel = { CounterViewModel() },
                    onEvent = { event ->
                        when (event) {
                            is CounterEvent.LimitReached -> Toast.makeText(
                                this@MainActivity,
                                "Limit: ${event.limit}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
            }
        }
    }
}
```

`viewModel` es una lambda que crea el ViewModel. Usa ahí tu framework de inyección de dependencias, o créalo directamente
como arriba: mira [Inyección de dependencias](guides/dependency-injection.md). `onEvent` recibe todos los eventos de la
pantalla, para mostrar un mensaje, como aquí, o para navegar. Más en [Pantallas en Compose](compose/screens.md).

### La preview

`onState` solo pinta el estado que recibe, así que una preview es una sola llamada: escríbela dentro del screen content,
dale cualquier estado y un `EmaActionDispatcher.EMPTY`, que ignora las acciones. Sin ViewModel ni fuente de datos falsa:

```kotlin
class CounterScreenContent : EmaScreenContent<CounterState, CounterAction> {

    // onState, como arriba

    @PreviewLightDark
    @Composable
    private fun PreviewCounter() {
        MaterialTheme {
            onState(state = CounterState.DEFAULT.copy(count = 7), actions = EmaActionDispatcher.EMPTY)
        }
    }
}
```

El IDE crea el screen content para dibujar la preview, así que deja su constructor sin parámetros. Añade una preview por
cada estado interesante (vacío, cargando, un diálogo de error...) para ver todos los casos de la pantalla a la vez.

## Qué ha pasado

1. El usuario toca el botón: la pantalla llama a `actions.dispatch(Increment)`.
2. El reducer del ViewModel calcula el siguiente estado.
3. La pantalla recibe el nuevo estado y se recompone.
4. En el décimo toque el side effect también publica `LimitReached`, que la pantalla muestra una vez y consume.

Continúa con [Por qué Emanate](why-emanate.md) para ver qué más pueden hacer las mismas piezas, o con
[Arquitectura](concepts/architecture.md).
