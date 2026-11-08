# Eventos

Un evento es **algo que ha ocurrido en la funcionalidad** y que la vista debe gestionar **una sola vez**:
mostrar un mensaje, abrir otra pantalla, cerrar la pantalla.

```kotlin
sealed interface HomeEvent : EmaEvent {
    data class FriendsUpdated(val count: Int) : HomeEvent
    data class FriendAdded(val friend: User) : HomeEvent
    data class FriendSelected(val friend: User) : HomeEvent
}
```

## Publicar eventos

Los eventos se publican desde los side effects del ViewModel, con `publish`:

```kotlin
sideEffects {
    add<HomeAction.SideEffects.FriendsLoaded> { action -> publish(HomeEvent.FriendsUpdated(action.friends.size)) }
    add<HomeAction.FriendClicked> { action -> publish(HomeEvent.FriendSelected(action.friend)) }
}
```

Un evento llega después de las acciones enviadas antes que él, así que la vista ve el estado que produjeron.

## Gestionar los eventos

La vista gestiona cada evento una vez, en el orden en que se publicaron. Una pantalla Compose los gestiona todos en un
único sitio: la lambda `onEvent` de `emaScreen`, en el grafo de navegación. Ahí tienes a mano el `NavController`, el
navigator y el `Context`, así que puede navegar, mostrar un mensaje o hacer cualquier otra cosa:

```kotlin
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            val navigator = remember { HomeNavigator(navController) }
            NavHost(navController, startDestination = HomeScreenContent::class.routeId) {
                emaScreen(
                    screenContent = HomeScreenContent(),
                    viewModel = { createHomeViewModel() },
                    onEvent = { event ->
                        when (event) {
                            is HomeEvent.FriendSelected -> navigator.goToProfile(event.friend)
                            is HomeEvent.FriendsUpdated -> toast("${event.count} amigos")
                            is HomeEvent.FriendAdded -> toast("${event.friend.name} ya es tu amigo")
                        }
                    }
                )
            }
        }
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
```

Como todos los eventos llegan al mismo `when`, el compilador te avisa cuando un evento nuevo no está gestionado. Si el
`when` crece, muévelo a una función del navigator. Mira [Pantallas en Compose](../compose/screens.md#gestionar-los-eventos).

## Nombres

**Los eventos dicen lo que ha pasado, nunca lo que hay que hacer.** El receptor decide la reacción.

| Prefiere                | Evita                 | Por qué                                                           |
|-------------------------|-----------------------|-------------------------------------------------------------------|
| `FriendSelected(user)`  | `OpenProfile(user)`   | El ViewModel no sabe que seleccionar significa navegar.           |
| `LoginSuccess(user)`    | `GoToHome`            | Qué pantalla sigue es decisión del navigator.                     |
| `CloseClicked`          | `CloseScreen`         | La vista también podría simplemente ocultar un panel.             |
| `FriendAdded(user)`     | `ShowToast(user)`     | Podría ser un toast, un snackbar o nada en absoluto.              |

Una buena prueba: el nombre sigue teniendo sentido si cambias por completo la UI de la pantalla.

## Reglas de entrega

- **Una vez.** Cuando `onEvent` termina, el evento queda consumido. No se reenvía cuando se recrea la vista.
- **Sin perderse.** Los eventos se encolan en el ViewModel hasta que una vista pueda recibirlos. Si publicas un evento
  mientras la pantalla está en segundo plano, o mientras se recrea, se entrega después.
- **En orden.** Varios eventos pendientes se entregan en bloque, en el orden en que se publicaron.
- **Eventos iguales.** Mientras un evento espera a ser entregado, publicar otro igual no hace nada, así que un doble
  toque no muestra el mismo mensaje dos veces. Cuando la vista ya lo ha gestionado, un evento igual es uno nuevo y se
  entrega de nuevo. Para encolar un evento igual mientras el primero sigue esperando, pasa `allowDuplicated = true`:

```kotlin
publish(HomeEvent.FriendAdded(ana), allowDuplicated = true)
```

- **Solo mientras la pantalla es visible.** Las pantallas Compose reciben eventos mientras el ciclo de vida está en
  `STARTED`.
- **No se persisten.** Los eventos viven en el ViewModel, así que sobreviven a los cambios de configuración pero no a la
  muerte del proceso.

> **Mantén `onEvent` corto.** Los eventos se entregan uno tras otro, y un evento se consume cuando su `onEvent` termina.
> Navegar o mostrar un toast es inmediato. Para algo que tarda, como un snackbar que espera al usuario, lánzalo en un
> scope de corrutinas tuyo, como el de `rememberCoroutineScope()`, para que los siguientes eventos no esperen por él.
> Devuelve el trabajo largo al ViewModel como una acción.

## Eventos o estado

Si debe estar ahí cuando el usuario vuelve, es [estado](state.md), no un evento.
Mira la tabla en [Arquitectura](architecture.md#estado-o-evento).

## Pantallas sin eventos

Una pantalla que nunca publica eventos usa `EmaEvent.EMPTY` como tipo de evento, y su `onEvent` no hace nada:

```kotlin
class SettingsViewModel : EmaViewModel<SettingsState, SettingsAction, EmaEvent.EMPTY>(SettingsState.DEFAULT) {
    // ...
}

emaScreen(
    screenContent = SettingsScreenContent(),
    viewModel = { SettingsViewModel() },
    onEvent = {}
)
```

Del mismo modo, `EmaState.EMPTY` es el estado de una pantalla sin estado, y `EmaAction.EMPTY` las acciones de una
pantalla con la que el usuario no puede interactuar. Una pantalla sin estado, acciones ni eventos no tiene lógica:
píntala con un composable normal, sin Emanate.
