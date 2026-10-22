# Dependency injection

Emanate does not depend on any dependency injection framework. Wherever it needs a ViewModel, it asks you for it with a
function, so you can use Koin, Hilt, Kodein, a manual container or plain constructors.

```kotlin
createComposableScreen(
    screenContent = HomeScreenContent(),
    viewModel = { HomeViewModel(getFriendsUseCase, HomeState()) },
    onEvent = { /* ... */ }
)
```

## One ViewModel per screen

Emanate calls the function once, when the screen is opened for the first time, and keeps the ViewModel while the screen
exists: see [How the ViewModel is kept alive](../concepts/viewmodel.md#the-viewmodel-and-its-screen).
The function must create the ViewModel, not return one that already exists, so each screen gets its own. A singleton
would be shared by every screen of that type and would keep the state of a screen that was already closed.

## With Koin

This is how the `sample/` app does it. Start Koin after initializing Emanate:

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

Declare the ViewModels as `factory`, so each screen gets its own. The initial state can be injected too, which
keeps the default state in one place:

```kotlin
val uiModule = module {
    factoryOf(::HomeViewModel)
    factoryOf(::ProfileViewModel)

    factory { HomeState() }
    factory { ProfileState() }
}
```

```kotlin
createComposableScreen(
    screenContent = HomeScreenContent(),
    viewModel = { get<HomeViewModel>() },
    onEvent = { /* ... */ }
)
```

## The store of a backup

A ViewModel that saves part of its state with a [backup](saving-state.md) receives its `EmaStateStore` in the
constructor. The function that creates the ViewModel receives the store of the screen, so pass it on:

```kotlin
class HomeViewModel(
    initialDataState: HomeState,
    private val stateStore: EmaStateStore
) : EmaViewModel<HomeState, HomeAction, HomeEvent>(initialDataState) {

    override fun setup() = source {
        plugins {
            +backupOf(stateStore) { /* ... */ }
        }
    }
}

createComposableScreen(
    screenContent = HomeScreenContent(),
    viewModel = { store -> HomeViewModel(HomeState(), store) },
    onEvent = { /* ... */ }
)
```

With Koin, pass it as a parameter:

```kotlin
val uiModule = module {
    factory { (stateStore: EmaStateStore) -> HomeViewModel(get(), stateStore) }
}

viewModel = { store -> get<HomeViewModel> { parametersOf(store) } }
```

On the platforms without a store, or in a test that doesn't check the saved state, pass `EmaStateStore.None`.

## Data and use cases

Repositories are usually singletons and use cases are created when needed:

```kotlin
val dataModule = module {
    single<Repository> { MockRepository() }
}

val useCaseModule = module {
    factory { GetFriendsUseCase(get()) }
    factory { GetUserFriendsUseCase(get()) }
}
```

The ViewModels receive the use cases in the constructor, so they can receive fakes in the [tests](testing.md).

## Pieces by environment

Besides use cases, a ViewModel can receive its middlewares, reducers and side effects. Then the dependency injection of
each build type or flavor decides how the screen behaves: the real API in production, a mock in the demo build, extra
latency in QA. See [Mocking and environments](environments.md#7-choose-the-pieces-by-environment).

## Injecting the configuration

By default the ViewModels use the configuration set with `Emanate.init`, so they don't need to receive it. To inject it
instead, for example in a library that can't call `Emanate.init` or in a module with its own configuration, receive it
in the constructor and pass it to the ViewModel. See [Explicit configuration](configuration.md#explicit-configuration).

```kotlin
class HomeViewModel(
    private val getFriendsUseCase: GetFriendsUseCase,
    configuration: EmaConfiguration
) : EmaViewModel<HomeState, HomeAction, HomeEvent>(HomeState(), configuration)
```

With Koin, declare it as a `single`:

```kotlin
val uiModule = module {
    single { EmaConfiguration.Android(debug = BuildConfig.DEBUG) }
    factoryOf(::HomeViewModel)
}
```

With Hilt, provide it from a module, and add `@Inject` to the constructor of the ViewModel:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object EmanateModule {

    @Provides
    @Singleton
    fun provideConfiguration(): EmaConfiguration = EmaConfiguration.Android(debug = BuildConfig.DEBUG)
}
```

The use cases only receive the `io` dispatcher of the configuration, in the constructor:

```kotlin
val useCaseModule = module {
    factory { GetFriendsUseCase(get(), get<EmaConfiguration>().dispatchers.io) }
}
```
