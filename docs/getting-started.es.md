# Primeros pasos

## Requisitos

- `minSdk` 24 o superior en Android.
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

Añade las dependencias de tu tecnología de UI:

```kotlin
val emanateVersion = "7.0.0"

dependencies {
    // Compose (recomendado)
    implementation("com.github.carmabs.ema:emanate-android:$emanateVersion")
    implementation("com.github.carmabs.ema:emanate-compose:$emanateVersion")

    // Vistas de Android: Fragments y Activities
    implementation("com.github.carmabs.ema:emanate-android-view:$emanateVersion")
}
```

En un módulo de Kotlin puro, como un módulo `presentation` con los ViewModels, añade solo el núcleo, y `emanate-test` para
testearlos:

```kotlin
dependencies {
    implementation("com.github.carmabs.ema:emanate-core:$emanateVersion")
    testImplementation("com.github.carmabs.ema:emanate-test:$emanateVersion")
}
```

`emanate-core` es una librería Kotlin Multiplatform, así que también se puede usar desde el `commonMain` de un módulo
multiplataforma.

## Inicializar Emanate

Llama a `Emanate.init` una vez, cuando arranca la aplicación y antes de crear ningún ViewModel.
`EmaConfiguration.Android(debug)` es la configuración para apps Android:

```kotlin
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Emanate.init(EmaConfiguration.Android(debug = BuildConfig.DEBUG))
    }
}
```

Registra la aplicación en el `AndroidManifest.xml` (`android:name=".MyApplication"`).
La configuración decide los dispatchers, qué pasa con las excepciones y dónde van los logs: con `BuildConfig.DEBUG`,
las builds de debug registran y fallan en el momento, y las de release no registran nada y siguen funcionando. Consulta [Configuración](guides/configuration.es.md).

## Tu primera pantalla

Una pantalla se compone de un **estado**, las **acciones** que puede realizar el usuario, los **eventos** que puede emitir
y un **ViewModel** que los conecta. Esto es un contador.

```kotlin
data class CounterState(val count: Int = 0) : EmaState

sealed interface CounterAction : EmaAction {
    data object Increment : CounterAction
}

sealed interface CounterEvent : EmaEvent {
    data class LimitReached(val limit: Int) : CounterEvent
}

class CounterViewModel :
    EmaViewModel<CounterState, CounterAction, CounterEvent>(CounterState()) {

    override fun setup() = source {
        reducers {
            add<CounterAction> { action ->
                when (action) {
                    CounterAction.Increment -> state.copy(count = state.count + 1)
                }
            }
        }
        sideEffects {
            add<CounterAction.Increment> {
                if (state.count == LIMIT) postEvent(CounterEvent.LimitReached(LIMIT))
            }
        }
    }

    private companion object {
        const val LIMIT = 10
    }
}
```

- El reducer devuelve el siguiente estado para cada acción, y se avisa a la vista.
- El side effect se ejecuta después del reducer, con el nuevo estado, y `postEvent(...)` envía un evento de un solo uso a
  la vista.
- El ViewModel nunca toca clases de Android, así que se puede testear en la JVM.

`EmaViewModel` es el ViewModel recomendado. También está disponible el clásico `EmaViewModelAction`, que actualiza el
estado de forma imperativa en `onAction`: consulta [ViewModel](concepts/viewmodel.es.md).

### La pantalla

Una pantalla Compose se describe con un `EmaComposableScreenContent`:

```kotlin
class CounterScreenContent :
    EmaComposableScreenContent<CounterState, CounterAction, CounterEvent> {

    @Composable
    override fun onState(
        state: CounterState,
        actions: EmaImmutableActionDispatcher<CounterAction>
    ) {
        Column {
            Text(text = state.count.toString())
            Button(onClick = { actions.dispatch(CounterAction.Increment) }) {
                Text(text = "+")
            }
        }
    }

    override suspend fun onEvent(
        context: Context,
        event: CounterEvent,
        actions: EmaImmutableActionDispatcher<CounterAction>
    ) {
        when (event) {
            is CounterEvent.LimitReached ->
                Toast.makeText(context, "Límite: ${event.limit}", Toast.LENGTH_SHORT).show()
        }
    }
}
```

Y se añade al grafo de navegación de una activity:

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
                createComposableScreen(
                    screenContent = CounterScreenContent(),
                    viewModel = { CounterViewModel() },
                    onEvent = { event ->
                        // Aquí van las decisiones de navegación
                    }
                )
            }
        }
    }
}
```

`viewModel` es una lambda que crea el ViewModel. Usa ahí tu framework de inyección de dependencias, o créalo directamente
como arriba: consulta [Inyección de dependencias](guides/dependency-injection.es.md). Más en
[Pantallas de Compose](compose/screens.es.md).

Si construyes la UI con Fragments y Activities, la misma pantalla se escribe como en
[Vistas de Android](android-view/screens.es.md).

## Qué ha pasado

1. El usuario pulsa el botón: la pantalla llama a `actions.dispatch(Increment)`.
2. El reducer del ViewModel calcula el siguiente estado.
3. La pantalla recibe el nuevo estado y se recompone.
4. En la décima pulsación el side effect además publica `LimitReached`, que la pantalla muestra una vez y consume.

Sigue con [Arquitectura](concepts/architecture.es.md).
