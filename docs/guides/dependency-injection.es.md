# Inyección de dependencias

Emanate no depende de ningún framework de inyección de dependencias. Allí donde necesita un ViewModel, te lo pide con una
función, así que puedes usar Koin, un contenedor manual o constructores normales.

```kotlin
// Compose
createComposableScreen(
    screenContent = LoginScreenContent(),
    viewModel = { LoginViewModel(loginUseCase, LoginState.DEFAULT) },
    onEvent = { /* ... */ }
)

// Vistas de Android
override fun provideViewModel(store: EmaStateStore): LoginViewModel = LoginViewModel(loginUseCase, LoginState.DEFAULT)
```

## Un ViewModel por pantalla

Emanate llama a la función una vez, cuando se abre la pantalla por primera vez, y conserva el ViewModel mientras la pantalla
existe: consulta [Cómo se conserva el ViewModel](../concepts/viewmodel.es.md#cómo-se-conserva-el-viewmodel).
La función tiene que crear el ViewModel, no devolver uno que ya exista, para que cada pantalla tenga el suyo. Un
singleton se compartiría entre todas las pantallas de ese tipo y conservaría el estado de una pantalla que ya se cerró.

## Con Koin

Así lo hace la app `sample`. Arranca Koin después de inicializar Emanate:

```kotlin
class EmaSampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Emanate.init(EmaConfiguration.Android(debug = BuildConfig.DEBUG))
        startKoin {
            androidContext(this@EmaSampleApplication)
            modules(dataModule, useCaseModule, uiModule)
        }
    }
}
```

Declara los ViewModels como `factory`, para que cada pantalla tenga el suyo. El estado inicial también se puede
inyectar, lo que mantiene el estado por defecto en un solo sitio:

```kotlin
val uiModule = module {
    factoryOf(::LoginViewModel)
    factoryOf(::HomeViewModel)

    factory { LoginState.DEFAULT }
    factory { HomeState.DEFAULT }
}
```

```kotlin
// Compose
createComposableScreen(
    screenContent = ProfileCreationScreenContent(),
    viewModel = { get<ProfileCreationViewModel>() },
    onEvent = { /* ... */ }
)

// Vistas de Android
override fun provideViewModel(store: EmaStateStore): LoginViewModel = get()
```

## El store de un backup

Un ViewModel que guarda parte de su estado con un backup recibe su `EmaStateStore` en el constructor. La función que
crea el ViewModel recibe el store de la pantalla, así que pásaselo:

```kotlin
class ProfileCreationViewModel(
    initialDataState: ProfileCreationState,
    private val stateStore: EmaStateStore
) : EmaViewModel<ProfileCreationState, ProfileCreationAction, ProfileCreationEvent>(initialDataState) {

    override fun setup() = source {
        install(backupOf(stateStore) { /* ... */ })
    }
}

// Compose
createComposableScreen(
    screenContent = ProfileCreationScreenContent(),
    viewModel = { store -> ProfileCreationViewModel(ProfileCreationState.DEFAULT, store) },
    onEvent = { /* ... */ }
)

// Vistas de Android
override fun provideViewModel(store: EmaStateStore) = ProfileCreationViewModel(ProfileCreationState.DEFAULT, store)
```

Con Koin, pásalo como parámetro:

```kotlin
val uiModule = module {
    factory { (stateStore: EmaStateStore) -> ProfileCreationViewModel(get(), stateStore) }
}

viewModel = { store -> get<ProfileCreationViewModel> { parametersOf(store) } }
```

En las plataformas sin store, o en un test que no comprueba el estado guardado, pasa `EmaStateStore.None`.

## Datos y casos de uso

Los repositorios suelen ser singletons y los casos de uso se crean cuando se necesitan:

```kotlin
val dataModule = module {
    single<Repository> { MockRepository() }
}

val useCaseModule = module {
    factory { LoginUseCase(get()) }
    factory { GetUserFriendsUseCase(get()) }
}
```

Los ViewModels reciben los casos de uso en el constructor, así que en los [tests](testing.es.md) pueden recibir
implementaciones falsas.

## Inyectar la configuración

Por defecto los ViewModels usan la configuración fijada con `Emanate.init`, así que no necesitan recibirla. Para
inyectarla, por ejemplo en una librería que no puede llamar a `Emanate.init` o en un módulo con su propia configuración,
recíbela en el constructor y pásasela al ViewModel. Consulta
[Configuración explícita](configuration.es.md#configuración-explícita).

```kotlin
class LoginViewModel(
    private val loginUseCase: LoginUseCase,
    configuration: EmaConfiguration
) : EmaViewModel<LoginState, LoginAction, LoginEvent>(LoginState.DEFAULT, configuration)
```

Con Koin, declárala como `single`:

```kotlin
val uiModule = module {
    single { EmaConfiguration.Android(debug = BuildConfig.DEBUG) }
    factoryOf(::LoginViewModel)
}
```

Los casos de uso solo reciben el dispatcher `io` de la configuración, en el constructor:

```kotlin
val useCaseModule = module {
    factory { LoginUseCase(get(), get<EmaConfiguration>().dispatchers.io) }
}
```
