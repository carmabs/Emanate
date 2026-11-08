# Inicializadores: pasar datos a una pantalla

Un **inicializador** son los datos que una pantalla necesita para empezar. Viaja en la ruta del destino y llega al
ViewModel como una acción, **una sola vez**, la primera vez que se crea la pantalla.

## Declararlo

```kotlin
@Serializable
sealed interface HomeInitializer : EmaInitializer {

    @Serializable
    data class HomeUser(val user: User) : HomeInitializer
}
```

Un tipo `sealed` permite que una pantalla empiece de distintas formas. Una pantalla que no recibe nada obtiene
`EmaInitializer.EMPTY`.

## Usarlo en el ViewModel

El inicializador es una acción más: un reducer aplica sus datos al estado, y un side effect carga lo que necesita la
pantalla.

```kotlin
override fun setup() = source {
    reducers {
        add<HomeInitializer> { initializer ->
            when (initializer) {
                is HomeInitializer.HomeUser -> state.copy(
                    userData = HomeState.UserData(initializer.user.name, initializer.user.surname, initializer.user.role)
                )
            }
        }
    }
    sideEffects {
        add<HomeInitializer> { initializer ->
            when (initializer) {
                is HomeInitializer.HomeUser -> getFriendsUseCase(Unit).onSuccess {
                    dispatch(HomeAction.SideEffects.FriendsLoaded(it))
                }
            }
        }
    }
}
```

Llega una sola vez, así que una rotación ni lo aplica ni vuelve a cargar los datos. Tras la muerte del proceso, la
pantalla lo recibe de nuevo, y un [backup](process-death.md) restaura encima lo que el usuario había escrito.

## Enviarlo y recibirlo

El inicializador viaja como texto dentro de la ruta, serializado con kotlinx.serialization: anótalo con
`@Serializable` y se permite cualquier carácter en sus valores.

- La pantalla que navega lo envía con `NavController.navigate(route, initializerBundle)`.
- El destino declara cómo leerlo con `initializerSupport`.

```kotlin
// Enviar
navController.navigate(
    route = HomeScreenContent::class.routeId,
    initializerBundle = EmaInitializerBundle(
        HomeInitializer.HomeUser(user),
        BundleSerializerStrategy.kSerialization(HomeInitializer.serializer())
    )
)

// Recibir
emaScreen(
    screenContent = HomeScreenContent(),
    viewModel = { get<HomeViewModel>() },
    initializerSupport = EmaInitializerSupport.fromRoute(HomeInitializer.serializer()),
    onEvent = { navigator.handleHomeEvent(it) { /* mostrar el mensaje */ } }
)
```

Los detalles, incluida la primera pantalla de una activity, están en
[Navegación en Compose](../compose/navigation.md#pasar-un-inicializador).

## Testearlo

En un test, llama a `onCreated` con el inicializador, como hace la pantalla:

```kotlin
viewModel.onCreated(HomeInitializer.HomeUser(ana))
```
