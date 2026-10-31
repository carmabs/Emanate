# Botón atrás en Compose

El usuario puede volver atrás de dos formas: pulsando la **flecha** que dibujas en la toolbar, o usando el **atrás del
sistema** (el botón o el gesto del móvil). Por defecto, el atrás del sistema simplemente cierra la pantalla. Esta
página explica cómo tomar el control, por ejemplo para preguntar *"¿Seguro que quieres salir?"* antes de que la
pantalla desaparezca.

## La idea en un minuto

En cada cambio del estado, Emanate le hace una pregunta al contenido de tu pantalla:

> *"Si el usuario pulsa atrás ahora mismo, ¿debo hacer algo especial?"*

Respondes implementando `onBack`, que recibe el [estado](../concepts/state.md) actual y devuelve:

| Devuelves | Qué ocurre cuando el usuario pulsa atrás |
|-----------|-------------------------------------------|
| Una **acción** | Emanate bloquea el comportamiento por defecto y envía esa acción al ViewModel. El ViewModel decide qué hacer. |
| `null` | Emanate no hace nada y el sistema gestiona el atrás como siempre (cierra la pantalla). |

Como la implementación base devuelve `null`, **una pantalla que no sobrescribe `onBack` se comporta como cualquier otra
pantalla de Android**. Solo lo escribes cuando necesitas algo distinto.

```kotlin
override fun onBack(state: ProfileCreationState): ProfileCreationAction? =
    if (state.dialog == null) ProfileCreationAction.OnBack else null
```

Léelo así: *"si no hay ningún diálogo abierto, convierte el atrás en la acción `OnBack`; si lo hay, déjalo al sistema"*.

## Qué hace Emanate por ti

Por debajo, Emanate instala un único `BackHandler` (la herramienta estándar de Compose para interceptar el atrás) y
llama a tu `onBack` cada vez que cambia el estado:

```kotlin
val backAction = screenContent.onBack(state)
BackHandler(enabled = backAction != null) {
    backAction?.also(actions::dispatch)
}
```

Por eso nunca creas un `BackHandler` tú mismo. Solo decides, estado a estado, **si interceptar y con qué acción**.
Devolver `null` en algunos estados es la forma de desactivar la interceptación.

!!! note "Por qué recibe el estado"
    Como `onBack` recibe el estado, la decisión puede depender de él. Un formulario puede interceptar el atrás solo
    cuando hay cambios sin guardar, y dejar salir libremente al usuario cuando todo está guardado.

## Ejemplo 1: confirmar antes de salir

La pantalla de creación de perfil tiene un formulario. Si el usuario pulsa atrás, no queremos que pierda por accidente
lo que ha escrito, así que primero mostramos un diálogo de confirmación.

![Flujo de una pulsación de atrás que abre un diálogo de confirmación](../assets/compose-back.svg)

El flujo tiene cuatro pasos:

1. El usuario pulsa atrás (la flecha o el atrás del sistema).
2. `onBack` devuelve `OnBack`, así que Emanate envía esa acción al ViewModel.
3. El ViewModel pone el diálogo en el estado y la pantalla lo dibuja.
4. Si el usuario confirma, el ViewModel publica un evento y el navigator cierra la pantalla.

### Paso 1: el contenido de la pantalla

Las dos formas de volver atrás envían **la misma acción**. La flecha lo hace en su `onClick`, y el atrás del sistema
a través de `onBack`:

```kotlin
class ProfileCreationScreenContent : EmaScreenContent<ProfileCreationState, ProfileCreationAction> {

    // Atrás del sistema
    override fun onBack(state: ProfileCreationState): ProfileCreationAction? =
        if (state.dialog == null) ProfileCreationAction.OnBack else null

    @Composable
    override fun onState(state: ProfileCreationState, actions: EmaActionDispatcher<ProfileCreationAction>) {
        // Flecha de la toolbar
        IconButton(onClick = { actions.dispatch(ProfileCreationAction.OnBack) }) { /* icono de flecha */ }

        // ... resto de la pantalla y el diálogo
    }
}
```

### Paso 2: el ViewModel

El ViewModel no sabe (ni le importa) si la acción vino de la flecha o del atrás del sistema. Reacciona a `OnBack`
abriendo el diálogo, y a la confirmación pidiendo al navigator que salga:

```kotlin
reducers {
    add<ProfileCreationAction> { action ->
        when (action) {
            is ProfileCreationAction.OnBack -> state.copy(dialog = ProfileCreationDialog.BackConfirmation)
            is ProfileCreationAction.DialogBackConfirm -> state.copy(dialog = null)
            is ProfileCreationAction.DialogBackCancel -> state.copy(dialog = null)
            // ...
        }
    }
}
sideEffects {
    // El usuario confirmó: el navigator cierra la pantalla
    add<ProfileCreationAction.DialogBackConfirm> { postEvent(ProfileCreationEvent.DialogConfirmationAccepted) }
}
```

### Paso 3: el navigator cierra la pantalla

El ViewModel no puede cerrar una pantalla por sí mismo: publica un [evento](../concepts/events.es.md), y el código que
registra la pantalla lo convierte en `navigateBack()`. Consulta [Navegación](navigation.es.md).

```kotlin
onEvent = { event ->
    when (event) {
        ProfileCreationEvent.DialogConfirmationAccepted -> navController.navigateBack()
    }
}
```

## ¿Por qué devolver `null` con el diálogo abierto?

Fíjate otra vez en la condición: `if (state.dialog == null) OnBack else null`.

Cuando el diálogo está en pantalla y el usuario pulsa atrás, **el propio diálogo debe gestionarlo** (normalmente
cerrándose, que es lo que el usuario espera). Los diálogos pintados con `AlertDialog` o `Dialog` ya lo hacen, y te lo
comunican con `onBackPressed` en su listener, como se explica en [Diálogos](dialogs.es.md).

Si `onBack` siguiera devolviendo `OnBack` con el diálogo abierto, la pantalla interceptaría el atrás *otra vez* y el
ViewModel abriría un segundo diálogo sobre el primero. Devolver `null` en ese estado lo evita.

| Estado | `onBack` devuelve | El atrás va a |
|--------|-------------------|---------------|
| Sin diálogo | `OnBack` | El ViewModel, que abre el diálogo |
| Diálogo abierto | `null` | El diálogo, que se cierra solo |

## Ejemplo 2: un atrás que simplemente vuelve

No todas las pantallas necesitan confirmación. En la pantalla de onboarding, atrás simplemente sale. Aun así, es mejor
enviar una acción que dejar que lo haga el sistema, para que haya **un único camino a través del ViewModel** tanto para
la flecha como para el atrás del sistema:

```kotlin
// Contenido de la pantalla: la flecha y el atrás del sistema envían la misma acción
override fun onBack(state: ProfileOnBoardingState) = ProfileOnBoardingActions.BackClicked

// ViewModel
sideEffects {
    add<ProfileOnBoardingActions.BackClicked> { postEvent(ProfileOnBoardingEvent.OnBoardingCancelled) }
}
```

El navigator convierte `OnBoardingCancelled` en `navigateBack()`. Tener un único camino tiene dos ventajas: puedes
añadir lógica después (analítica, guardar un borrador...) en un solo sitio, y un [test](../guides/testing.es.md) puede
comprobar el comportamiento sin dibujar la pantalla: envía `BackClicked` y verifica el evento.

## ¿Cuál necesito?

| Quiero... | Haz esto |
|-----------|----------|
| Nada especial, que la pantalla se cierre | No sobrescribas `onBack` (devuelve `null`) |
| Que el ViewModel sepa que el usuario salió | Devuelve siempre una acción y publica el evento de navegación desde el ViewModel |
| Pedir confirmación antes de salir | Devuelve la acción solo cuando haya algo que proteger y muestra un diálogo |
| Que el atrás funcione normal con un diálogo abierto | Devuelve `null` cuando el estado tenga un diálogo |
