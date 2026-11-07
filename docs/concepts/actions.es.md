# Acciones

Una acción es **algo que ha hecho el usuario**: "tiró para refrescar", "escribió una consulta", "confirmó el diálogo".
Las acciones van de la vista al ViewModel.

```kotlin
sealed interface HomeAction : EmaAction {

    data object RefreshRequested : HomeAction
    data class QueryWritten(val query: String) : HomeAction
    data class FriendClicked(val friend: User) : HomeAction
    data object ErrorAccepted : HomeAction
}
```

## Nombres

Nombra las acciones por **lo que ha hecho el usuario**, en pasado, no por lo que debería hacer el ViewModel.
`QueryWritten` es mejor que `UpdateQuery`: la vista comunica el hecho y el ViewModel decide qué hacer con él.

Usa una `sealed interface` para que el `when` sea exhaustivo. Cuando añades una acción el compilador te dice en qué
sitios debes gestionarla.

## Gestionar acciones

Declara en el `setup` del ViewModel los middlewares, reducers y side effects de cada tipo de acción. Con el tipo
`sealed`, un reducer gestiona todas las acciones de la pantalla en un `when` exhaustivo, y un side effect puede gestionar
una sola acción:

```kotlin
override fun setup() = source {
    reducers {
        add<HomeAction> { action ->
            when (action) {
                is HomeAction.QueryWritten -> state.copy(query = action.query)
                is HomeAction.ErrorAccepted -> state.copy(dialog = null)
                // ...
            }
        }
    }
    sideEffects {
        add<HomeAction.RefreshRequested> { /* ... */ }
    }
}
```

Mira [El setup](viewmodel.md#el-setup). El punto de entrada público del ViewModel es `dispatch(action)`, así que una vista
solo puede comunicarse mediante acciones y no puede llamar a funciones arbitrarias del ViewModel.

## Las acciones también son resultados

Los side effects también comunican lo ocurrido con acciones, para que los reducers puedan aplicarlo. Agrúpalas en un
tipo sealed dentro de las acciones de la pantalla, llamado `SideEffects`, para que quede claro que solo las envía el
ViewModel. Nombra cada acción por lo que ocurrió (`QueryLoaded`):

```kotlin
sealed interface HomeAction : EmaAction {
    // ...

    // Los resultados de la búsqueda, enviados por su side effect
    sealed interface SideEffects : HomeAction {
        data object QueryStarted : SideEffects
        data class QueryLoaded(val friends: List<User>) : SideEffects
        data object QueryFailed : SideEffects
    }
}
```

Como cada cambio del estado viene de una acción, el log de las acciones cuenta toda la historia de la pantalla, y un test
puede comprobarla paso a paso.

## Enviar desde la vista

Un screen content de Compose recibe el estado y un `EmaActionDispatcher`, el mismo durante toda la vida de la pantalla,
para no causar recomposiciones innecesarias. Cada callback de la UI solo comunica lo que ha hecho el usuario con
`actions.dispatch`:

```kotlin
class HomeScreenContent : EmaScreenContent<HomeState, HomeAction> {

    @Composable
    override fun onState(state: HomeState, actions: EmaActionDispatcher<HomeAction>) {
        Column {
            // El usuario escribe una consulta
            OutlinedTextField(
                value = state.query,
                onValueChange = { query -> actions.dispatch(HomeAction.QueryWritten(query)) }
            )
            // El usuario pide un refresco
            Button(onClick = { actions.dispatch(HomeAction.RefreshRequested) }) {
                Text(text = "Refrescar")
            }
            // El usuario pulsa un amigo
            FriendList(
                friends = state.friends,
                onFriendClick = { friend -> actions.dispatch(HomeAction.FriendClicked(friend)) }
            )
        }
        // El usuario acepta el diálogo de error
        if (state.dialog == HomeDialog.LoadError) {
            AlertDialog(
                onDismissRequest = { actions.dispatch(HomeAction.ErrorAccepted) },
                confirmButton = {
                    TextButton(onClick = { actions.dispatch(HomeAction.ErrorAccepted) }) { Text(text = "OK") }
                },
                text = { Text(text = "No se pudieron cargar los amigos") }
            )
        }
    }
}
```

Los callbacks nunca cambian el estado ni deciden qué pasa después: lo hace el ViewModel, en sus reducers y side effects.
Los composables hijos como `FriendList` reciben callbacks normales, así que no dependen de Emanate y se pueden
reutilizar o previsualizar por separado.

Para las previews usa `EmaActionDispatcher.EMPTY`, que ignora todas las acciones.

## Tipos especiales de acción

| Tipo                      | Uso                                                               |
|---------------------------|-------------------------------------------------------------------|
| `EmaAction`               | Las acciones de una pantalla. Las tuyas extienden esta.           |
| `EmaInitializer`          | Los datos con los que empieza una pantalla. Mira [Inicializadores](../guides/initializers.md). |
| `EmaLifecycle`            | El ciclo de vida de la vista: `Started`, `Resumed`, `Paused`, `Stopped` y `Destroyed`. Mira [ViewModel](viewmodel.md#el-ciclo-de-vida-como-acciones). |
| `EmaRestoreState`         | Los valores restaurados por un [backup](../guides/process-death.md) tras la muerte del proceso. |
| `EmaAction.EMPTY`         | El tipo de acción de una pantalla sin acciones.                   |

Cualquiera de ellos puede ser gestionado por los middlewares, reducers y side effects, como las acciones de la pantalla.
