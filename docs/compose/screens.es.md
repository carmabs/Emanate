# Pantallas de Compose

Las pantallas de Compose se describen con un `EmaComposableScreenContent` y se registran en un grafo de navegación con
`createComposableScreen`. Necesitan los artefactos `emanate-android` y `emanate-compose-android`.

## Contenido de la pantalla

```kotlin
class ProfileOnBoardingScreenContent :
    EmaComposableScreenContent<ProfileOnBoardingState, ProfileOnBoardingActions, ProfileOnBoardingEvent> {

    @Composable
    override fun onState(
        state: ProfileOnBoardingState,
        actions: EmaImmutableActionDispatcher<ProfileOnBoardingActions>
    ) {
        // Pinta el estado. Envía lo que haga el usuario con actions.dispatch(...)
    }
}
```

`EmaComposableScreenContent<S, A, E>` tiene tres miembros:

| Miembro                                     | Para qué                                                                 |
|---------------------------------------------|--------------------------------------------------------------------------|
| `onState(state, actions)`                   | **Obligatorio.** Pinta el estado. Es un `@Composable`.                   |
| `suspend onEvent(context, event, actions)`  | Gestiona los [eventos](../concepts/events.es.md) que son cosa de la UI (mensajes, vibración...). Opcional. |
| `onBack(state): A?`                         | La acción que se envía al pulsar atrás, o `null`. Consulta [Botón atrás](back-handling.es.md). Opcional. |

Mantén el contenido de la pantalla sin estado: es una descripción sin estado, creada una vez por pantalla.
Todo lo que tenga que sobrevivir va en el [estado](../concepts/state.es.md) del ViewModel.

## Registrar la pantalla

```kotlin
NavHost(
    navController = navController,
    startDestination = ProfileOnBoardingScreenContent::class.routeId
) {
    createComposableScreen(
        screenContent = ProfileOnBoardingScreenContent(),
        viewModel = { get<ProfileOnBoardingViewModel>() },
        onEvent = { navigator.handleProfileOnBoardingEvent(it) }
    )
}
```

| Parámetro              | Para qué                                                                                       |
|------------------------|------------------------------------------------------------------------------------------------|
| `screenContent`        | El contenido de la pantalla.                                                                   |
| `viewModel`            | Una lambda que crea el ViewModel. Se llama una vez, al abrir la pantalla. Consulta [Inyección de dependencias](../guides/dependency-injection.es.md). |
| `onEvent`              | Recibe todos los eventos. Pon aquí la **navegación**, donde tienes el `NavController`.         |
| `routeId`              | La ruta. Por defecto `ScreenContent::class.routeId`, única por clase.                          |
| `initializerSupport`   | Cómo leer el [inicializador](../guides/initializers.es.md) de la ruta. Consulta [Navegación](navigation.es.md#pasar-un-inicializador). |
| `saveStateManager`     | Consulta [Guardar el estado si el proceso muere](save-state.es.md).                            |
| `transitionAnimation`  | `EmaComposableTransitions(enterTransition, exitTransition, popEnterTransition, popExitTransition)`. Las transiciones de vuelta son, por defecto, las de entrada y salida. |
| `fullScreenDialogMode` | Registra el destino como un `dialog` en lugar de un `composable`.                              |
| `decoration`           | Envuelve la pantalla, por ejemplo en un `Scaffold`. Recibe el contenido y el dispatcher de acciones. |
| `previewRenderState`   | El estado que se pinta en la preview del IDE.                                                  |
| `onViewModelInstance`  | Te da la instancia del ViewModel, por ejemplo para compartirla con otros composables.          |

## Dos sitios para gestionar los eventos

Cada evento llega a **los dos** manejadores, en este orden:

1. La lambda `onEvent` de `createComposableScreen` (sin contexto, pero con tu `NavController` a mano).
2. `screenContent.onEvent(context, event, actions)`.

Después el evento se consume. Una regla sencilla: la navegación en el grafo y todo lo demás en el contenido de la pantalla.

## Ciclo de vida

La pantalla conecta el ViewModel con el ciclo de vida de su destino:

| Evento del ciclo de vida       | ViewModel                       |
|--------------------------------|---------------------------------|
| `ON_CREATE`                    | `onCreated(initializer)`        |
| `ON_START`                     | `onStartView()`                 |
| `ON_RESUME`                    | `onResumeView()`                |
| `ON_PAUSE`                     | `onPauseView()`                 |
| `ON_STOP`                      | `onStopView()`                  |
| La pantalla sale de la composición | `onPauseView()` y `onStopView()` |

El estado se recoge con `collectAsStateWithLifecycle` y los eventos mientras el ciclo de vida está en `STARTED`.

## Previews

En la preview del IDE no hay ViewModel, así que llama a `onState` directamente con un estado y un dispatcher vacío:

```kotlin
@Preview
@Composable
private fun NormalPreview() {
    EmaSampleTheme {
        ProfileCreationScreenContent().onState(
            state = ProfileCreationState(Role.ADMIN, "Carlos", "Mateo"),
            actions = EmaImmutableActionDispatcher.EMPTY
        )
    }
}
```

`previewRenderState` en `createComposableScreen` hace lo mismo para toda la pantalla: en una preview no se crea el
ViewModel y la pantalla pinta ese estado. `EmaUtils.Preview.Compose.isInPreview()` y `skipForPreview { }` te ayudan a
saltarte el código que no puede ejecutarse en una preview. Consulta [Utilidades](utilities.es.md#previews).

## Usar una pantalla sin grafo de navegación

`EmaComposableScreen` es el composable que hay detrás de `createComposableScreen`. Úsalo para colocar una pantalla de Emanate
en cualquier sitio:

```kotlin
EmaComposableScreen(
    vm = { CounterViewModel() },
    screenContent = CounterScreenContent(),
    onEvent = { /* ... */ }
)
```

Acepta el `initializer`, un `saveStateSupport` (consulta [Guardar el estado](save-state.es.md#sin-grafo-de-navegación))
y un `previewRenderState`. Otra sobrecarga recibe una instancia del ViewModel y su dispatcher de acciones, para
ViewModels que creas y conservas tú.

## Tema

Envuelve el `NavHost` (o tu contenido) en tu tema de Material. El sample usa `EmaSampleTheme`.
