# Diálogos en Compose

Un **diálogo** es una ventana pequeña que aparece sobre la pantalla para contarle algo al usuario o hacerle una pregunta:
*"¿Borrar esta nota?"*, *"Ha fallado la conexión"*, *"¿Descartar los cambios?"*. Esta página explica cómo mostrar uno
con Emanate, paso a paso, con un ejemplo completo que puedes copiar.

## La idea en un minuto

En Emanate **un diálogo no es algo que se abre: es algo que dice el estado**. No existe `dialog.show()` ni
`dialog.dismiss()`. En su lugar:

1. El estado tiene un campo que dice *qué diálogo está abierto*, o `null` si no hay ninguno.
2. La pantalla pinta el diálogo **mientras** ese campo no sea `null`.
3. Los botones del diálogo no lo cierran. Envían una **acción**, y el ViewModel cambia el campo.

![Ciclo de un diálogo gobernado por el estado](../assets/compose-dialog.svg)

Si ya conoces el [estado](../concepts/state.es.md), las [acciones](../concepts/actions.es.md) y los
[reducers](../concepts/reducers.md), aquí no hay nada nuevo: un diálogo es una pieza más del estado.

!!! tip "¿Por qué hacerlo así?"
    - **Sobrevive a una rotación.** El estado lo conserva el ViewModel, así que el diálogo sigue ahí cuando se recrea
      la pantalla. Un diálogo abierto con `show()` desaparece.
    - **El ViewModel sabe qué hay en pantalla.** Puede decidir cosas como *"ignora este toque mientras haya un diálogo
      abierto"*.
    - **Se puede probar sin dispositivo.** Todo el flujo es estado y acciones: consulta [Probar un diálogo](#probar-un-diálogo).

## Ejemplo: confirmar antes de borrar una nota

Vamos a construir una pantalla con un botón *Borrar*. Cuando el usuario lo pulsa, un diálogo pide confirmación.

### Paso 1: describe los diálogos en el estado

Crea un tipo que liste **todos los diálogos que la pantalla puede mostrar**. Una `sealed interface` es perfecta: cada
diálogo es un caso, y el compilador te avisa si olvidas pintar alguno.

```kotlin
sealed interface NoteDialog {
    data object ConfirmDelete : NoteDialog
}

data class NoteState(
    val title: String = "",
    val dialog: NoteDialog? = null     // null = ningún diálogo abierto
) : EmaState
```

`dialog` es nullable a propósito: `null` significa *"no hay nada abierto"*.

### Paso 2: describe lo que puede hacer el usuario

Cada botón de la pantalla y del diálogo es una acción:

```kotlin
sealed interface NoteAction : EmaAction {
    data object DeleteClicked : NoteAction          // el botón Borrar de la pantalla
    data object DeleteConfirmed : NoteAction        // "Aceptar" en el diálogo
    data object DeleteCancelled : NoteAction        // "Cancelar" en el diálogo, o atrás, o tocar fuera
}

sealed interface NoteEvent : EmaEvent {
    data object NoteDeleted : NoteEvent
}
```

### Paso 3: el ViewModel abre y cierra el diálogo

Abrir un diálogo es escribirlo en el estado. Cerrarlo es escribir `null`:

```kotlin
class NoteViewModel(private val deleteNote: DeleteNoteUseCase) :
    EmaViewModel<NoteState, NoteAction, NoteEvent>(NoteState()) {

    override fun setup() = source {
        reducers {
            add<NoteAction> { action ->
                when (action) {
                    is NoteAction.DeleteClicked -> state.copy(dialog = NoteDialog.ConfirmDelete)   // abrir
                    is NoteAction.DeleteCancelled -> state.copy(dialog = null)                     // cerrar
                    is NoteAction.DeleteConfirmed -> state.copy(dialog = null)                     // cerrar
                }
            }
        }
        sideEffects {
            // El trabajo se hace tras confirmar: el diálogo ya está cerrado
            add<NoteAction.DeleteConfirmed> {
                deleteNote(Unit)
                postEvent(NoteEvent.NoteDeleted)
            }
        }
    }
}
```

Fíjate en que confirmar hace dos cosas, y cada una vive donde le corresponde: el **reducer** cierra el diálogo
(es estado) y el **efecto secundario** borra la nota (es trabajo). Consulta los [reducers](../concepts/reducers.md) y los
[efectos secundarios](../concepts/side-effects.md) si necesitas un recordatorio.

### Paso 4: la pantalla pinta el diálogo

En `onState`, comprueba el campo. Si hay un diálogo, emite el composable que lo pinta. Aquí usamos el
`AlertDialog` de Material 3, el diálogo estándar de Compose:

```kotlin
@Composable
override fun onState(state: NoteState, actions: EmaActionDispatcher<NoteAction>) {
    Column {
        Text(state.title)
        Button(onClick = { actions.dispatch(NoteAction.DeleteClicked) }) {
            Text("Borrar")
        }
    }

    // El diálogo se pinta solo mientras el estado lo indique
    when (state.dialog) {
        NoteDialog.ConfirmDelete -> AlertDialog(
            onDismissRequest = { actions.dispatch(NoteAction.DeleteCancelled) },   // atrás o tocar fuera
            title = { Text("Borrar nota") },
            text = { Text("No se puede deshacer. ¿Quieres continuar?") },
            confirmButton = {
                TextButton(onClick = { actions.dispatch(NoteAction.DeleteConfirmed) }) { Text("Borrar") }
            },
            dismissButton = {
                TextButton(onClick = { actions.dispatch(NoteAction.DeleteCancelled) }) { Text("Cancelar") }
            }
        )
        null -> Unit    // nada que pintar
    }
}
```

Eso es toda la funcionalidad. Sigamos lo que ocurre cuando el usuario pulsa *Borrar*:

| # | Qué ocurre | Quién |
|---|------------|-------|
| 1 | El usuario pulsa el botón. La pantalla envía `DeleteClicked`. | Pantalla |
| 2 | El reducer devuelve `state.copy(dialog = ConfirmDelete)`. | ViewModel |
| 3 | El estado cambió, así que `onState` se ejecuta de nuevo. Ahora `state.dialog` no es `null` y se pinta el `AlertDialog`. | Pantalla |
| 4 | El usuario pulsa *Borrar* en el diálogo. La pantalla envía `DeleteConfirmed`. | Pantalla |
| 5 | El reducer devuelve `dialog = null` y el efecto secundario borra la nota. `onState` se ejecuta de nuevo y el diálogo deja de pintarse. | ViewModel |

!!! warning "El error más común"
    El `onDismissRequest` de `AlertDialog` se llama cuando el usuario pulsa atrás o toca fuera del diálogo. **No** lo
    cierra. Si lo dejas vacío (`onDismissRequest = {}`), el diálogo no se podrá descartar de ese modo, y si intentas
    ocultarlo con un `var` local, el estado y la pantalla se desincronizan. **Envía siempre una acción** que
    ponga `dialog = null`.

## Diálogos con datos

Un diálogo suele necesitar información: ¿*qué* nota se va a borrar? Usa una `data class` en lugar de un `data object`
y mete los datos dentro:

```kotlin
sealed interface NoteListDialog {
    data class ConfirmDelete(val noteId: Int, val noteTitle: String) : NoteListDialog
    data class Error(val message: EmaText) : NoteListDialog
}
```

```kotlin
// Reducer: el diálogo se crea con los datos de la nota tocada
is NoteListAction.DeleteClicked -> state.copy(
    dialog = NoteListDialog.ConfirmDelete(action.note.id, action.note.title)
)
```

```kotlin
// Pantalla: cada caso del tipo sellado se pinta de forma distinta
when (val dialog = state.dialog) {
    is NoteListDialog.ConfirmDelete -> AlertDialog(
        onDismissRequest = { actions.dispatch(NoteListAction.DialogDismissed) },
        title = { Text("¿Borrar \"${dialog.noteTitle}\"?") },
        confirmButton = {
            TextButton(onClick = { actions.dispatch(NoteListAction.DeleteConfirmed(dialog.noteId)) }) {
                Text("Borrar")
            }
        },
        dismissButton = {
            TextButton(onClick = { actions.dispatch(NoteListAction.DialogDismissed) }) { Text("Cancelar") }
        }
    )
    is NoteListDialog.Error -> AlertDialog(
        onDismissRequest = { actions.dispatch(NoteListAction.DialogDismissed) },
        text = { Text(dialog.message.stringResource()) },
        confirmButton = {
            TextButton(onClick = { actions.dispatch(NoteListAction.DialogDismissed) }) { Text("OK") }
        }
    )
    null -> Unit
}
```

Como un `when` sobre una `sealed interface` debe ser exhaustivo, **al añadir un diálogo nuevo al tipo el compilador
te señala el lugar donde tienes que pintarlo**.

!!! note "Un solo diálogo a la vez"
    Un campo nullable solo puede guardar un diálogo. Casi siempre es lo que quieres, porque dos diálogos apilados
    confunden. Si de verdad necesitas dos a la vez, usa dos campos.

## Evita repetirte: un diálogo reutilizable

Cuando varios diálogos son iguales (un título, un mensaje, *Aceptar* y *Cancelar*), no escribas el `AlertDialog` una y
otra vez. Envuélvelo una vez en tu propio composable que reciba los textos y **dos lambdas**:

```kotlin
@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Aceptar") } },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancelar") } }
    )
}

// Uso
NoteDialog.ConfirmDelete -> ConfirmationDialog(
    title = "Borrar nota",
    message = "No se puede deshacer.",
    onConfirm = { actions.dispatch(NoteAction.DeleteConfirmed) },
    onCancel = { actions.dispatch(NoteAction.DeleteCancelled) }
)
```

## El diálogo y el botón atrás

Mientras haya un diálogo en pantalla, **el botón atrás lo debe gestionar el diálogo**, no la pantalla de detrás. Haz que
`onBack` devuelva `null` cuando `state.dialog` no sea `null`:

```kotlin
override fun onBack(state: NoteState): NoteAction? =
    if (state.dialog == null) NoteAction.BackClicked else null
```

Si lo olvidas, la pantalla intercepta atrás *y* el diálogo también, y el usuario puede ver dos diálogos uno sobre otro.
La explicación completa está en [Botón atrás](back-handling.es.md).

## Carga: no siempre es un diálogo

Un indicador de carga no tiene por qué ser un diálogo. En la mayoría de los casos es mejor modelarlo como un
**booleano** del estado y pintar un indicador de progreso donde corresponda, por ejemplo dentro del botón que inició el
trabajo:

```kotlin
data class LoginState(val isLoading: Boolean = false /* ... */) : EmaState

// Reducer
is LoginAction.LoginClicked -> state.copy(isLoading = true)
is LoginAction.LoginFinished -> state.copy(isLoading = false)
```

```kotlin
Button(onClick = { actions.dispatch(LoginAction.LoginClicked) }, enabled = !state.isLoading) {
    if (state.isLoading) CircularProgressIndicator(Modifier.size(18.dp)) else Text("Iniciar sesión")
}
```

Usa un *diálogo de carga* bloqueante solo cuando el usuario no deba tocar nada mientras se hace el trabajo. Aun así es el
mismo patrón: `isLoading` en el estado, y `if (state.isLoading) LoadingDialog()` en la pantalla.

## Probar un diálogo

Como el diálogo es estado y las elecciones son acciones, pruebas todo el flujo **sin pintar nada**: envía la acción y
comprueba el estado.

```kotlin
@Test
fun `pulsar borrar pide confirmación y confirmar borra la nota`() = runTest {
    val viewModel = NoteViewModel(FakeDeleteNoteUseCase())
    val recorder = viewModel.record()

    viewModel.dispatch(NoteAction.DeleteClicked)
    recorder.assertState { it.dialog == NoteDialog.ConfirmDelete }

    viewModel.dispatch(NoteAction.DeleteConfirmed)
    advanceUntilIdle()

    recorder.assertState { it.dialog == null }
    recorder.assertEvents(NoteEvent.NoteDeleted)
}
```

Consulta [Testing](../guides/testing.es.md) para saber cómo configurar `useTestConfiguration()`.
