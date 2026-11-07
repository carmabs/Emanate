# Estado

El estado es una `data class` inmutable con **todo lo que la pantalla necesita para pintarse**.

```kotlin
data class HomeState(
    val user: User? = null,
    val query: String = STRING_EMPTY,
    val friends: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val errorQueryTooShort: Boolean = false,
    val dialog: HomeDialog? = null
) : EmaState
```

## Reglas

- **Hazlo una `data class`.** `copy` es como se crea el siguiente estado, y su `equals` permite a la vista omitir los
  estados que no han cambiado. Usa `EmaState.EMPTY` para pantallas sin estado.
- **Hazlo inmutable.** Usa `val` y colecciones de solo lectura. Cámbialo con `copy`.
- **Dale valores por defecto** (o una instancia `DEFAULT`). Se convierten en el estado inicial y son útiles para
  previews y tests.
- **Añade propiedades derivadas en lugar de duplicar datos.** Mantienen la vista libre de lógica:

```kotlin
data class HomeState(
    val user: User? = null,
    val friends: List<User> = emptyList(),
    // ...
) : EmaState {

    val showCreateButton get() = user?.role == Role.ADMIN
    val showEmptyList get() = user != null && friends.isEmpty()
}
```

## Cambiar el estado

El estado solo cambia en los [reducers](reducers.md) del ViewModel, que devuelven el siguiente estado con `copy`:

```kotlin
reducers {
    add<HomeAction> { action ->
        when (action) {
            is HomeAction.QueryWritten -> state.copy(query = action.query, errorQueryTooShort = false)
            // ...
        }
    }
}
```

Los middlewares y los side effects pueden leer el valor actual con `state`, pero nunca cambiarlo:

```kotlin
sideEffects {
    add<HomeAction.RefreshRequested> {
        if (state.isLoading) return@add
        // ...
    }
}
```

El estado se expone como `stateFlow: StateFlow<S>`. Al ser un `StateFlow`, los estados consecutivos iguales no se
entregan dos veces y los colectores lentos solo ven el último valor.

## Modelar diálogos, carga y errores

Emanate no tiene estados especiales de "carga" o "error". Son campos normales de tu estado, así que sobreviven a un
cambio de configuración y son fáciles de testear.

**Un diálogo**: un tipo sealed nullable. `null` significa que no hay diálogo.

```kotlin
sealed interface HomeDialog {
    data object LoadError : HomeDialog
}

// Reducers
is HomeAction.SideEffects.QueryFailed -> state.copy(isLoading = false, dialog = HomeDialog.LoadError)  // mostrar
is HomeAction.ErrorAccepted -> state.copy(dialog = null)                                    // ocultar
```

**Carga**: un booleano, activado por la acción que inicia el trabajo y desactivado por la que comunica su resultado.

```kotlin
is HomeAction.SideEffects.QueryStarted -> state.copy(isLoading = true)
is HomeAction.SideEffects.QueryLoaded -> state.copy(isLoading = false, friends = action.friends)
```

**Errores de campo**: un booleano (o un mensaje) por campo, con el prefijo `error`. Actívalo cuando la entrada no sea
válida y límpialo cuando el usuario edite el campo:

```kotlin
// Reducers
is HomeAction.RefreshRequested -> state.copy(errorQueryTooShort = state.query.length == 1)  // mostrar
is HomeAction.QueryWritten -> state.copy(query = action.query, errorQueryTooShort = false)  // ocultar
```

La vista solo pinta lo que dice el campo:

```kotlin
@Composable
override fun onState(state: HomeState, actions: EmaActionDispatcher<HomeAction>) {
    OutlinedTextField(
        value = state.query,
        onValueChange = { query -> actions.dispatch(HomeAction.QueryWritten(query)) },
        isError = state.errorQueryTooShort,
        supportingText = {
            if (state.errorQueryTooShort) Text(text = "Escribe al menos dos letras")
        }
    )
}
```

## Pintar el estado

La vista simplemente pinta el estado que recibe. Con Compose, la recomposición se encarga de calcular las diferencias.
Mira [Pantallas en Compose](../compose/screens.md).

## Lo que *no* pertenece al estado

- Cosas de un solo uso: mensajes, navegación. Usa [eventos](events.md).
- Tipos de la plataforma, como un contexto, una vista o una imagen de la plataforma. Usa `EmaText` para textos con
  recursos, mira [Textos en el estado](../utils/texts.md).
- Colecciones u objetos mutables que cambian sin que llames a `copy`.
