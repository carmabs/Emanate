<img src="docs/assets/logo.svg" alt="Logo de Emanate" height="80" align="right">

# Emanate

[English](README.md) · **Español**

**Emanate** es una arquitectura unidireccional para Kotlin donde cada comportamiento de una pantalla es una pieza pequeña
que puedes sustituir, interceptar, reutilizar y testear por separado: un **estado** que pinta la vista, **acciones** que
realiza el usuario, **eventos** de un solo uso a los que reacciona la vista, y un ViewModel que gestiona cada acción con
**middlewares**, **reducers** y **side effects**. Los ViewModels son código Kotlin Multiplatform puro y las pantallas se
construyen con Jetpack Compose.

Como las piezas son valores, la misma pantalla puede llamar a la API real en producción, responder con datos simulados en
una build de demo, simular latencia o errores en QA y ejecutarse contra un fake en los tests, solo cambiando lo que se
inyecta.

```kotlin
data class CounterState(val count: Int) : EmaState {

    companion object {
        val DEFAULT = CounterState(count = 0)
    }
}

sealed interface CounterAction : EmaAction {
    data object Increment : CounterAction
}

sealed interface CounterEvent : EmaEvent {
    data class LimitReached(val limit: Int) : CounterEvent
}

class CounterViewModel :
    EmaViewModel<CounterState, CounterAction, CounterEvent>(CounterState.DEFAULT) {

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
                if (state.count == 10) publish(CounterEvent.LimitReached(10))
            }
        }
    }
}
```

## Instalación

```kotlin
// settings.gradle.kts
maven { url = uri("https://jitpack.io") }
```

```kotlin
dependencies {
    implementation("com.github.carmabs.emanate:emanate-android:7.0.0")
    implementation("com.github.carmabs.emanate:emanate-compose-android:7.0.0")
    // Solo si tus pantallas están en un grafo de Navigation Compose
    implementation("com.github.carmabs.emanate:emanate-compose-android-nav2:7.0.0")
}
```

En un módulo de Kotlin puro o multiplataforma usa `emanate-core`, y `emanate-test` para testear los ViewModels.
Después inicializa Emanate en tu `Application`:

```kotlin
Emanate.init(EmaConfiguration.Android(debug = BuildConfig.DEBUG))
```

## Documentación

La guía está en [`docs/`](docs/index.es.md):

- [Por qué Emanate](docs/why-emanate.es.md) y [primeros pasos](docs/getting-started.es.md)
- Conceptos: [arquitectura](docs/concepts/architecture.es.md), [estado](docs/concepts/state.es.md), [acciones](docs/concepts/actions.es.md), [eventos](docs/concepts/events.es.md), [ViewModel](docs/concepts/viewmodel.es.md), [source](docs/concepts/source.es.md)
- Guías: [configuración](docs/guides/configuration.es.md), [sobrevivir a la muerte del proceso](docs/guides/process-death.es.md), [inyección de dependencias y entornos](docs/guides/dependency-injection.es.md), [testing](docs/guides/testing.es.md) y más
- [Compose](docs/compose/screens.es.md): pantallas, navegación, botón atrás, diálogos y permisos
- [Recomendaciones](docs/recommendations.es.md) y [contribuir](docs/contributing.es.md)

## Ejemplo

La app [`sample/`](sample) muestra todas las funcionalidades juntas: un login con validación y diálogos, una lista con
roles, tres entornos (mock, dev y pro) elegidos con flavors y un flujo en Compose con navegación, botón atrás y estado guardado.
Abre la carpeta `sample` en Android Studio y ejecuta el módulo `app`.
