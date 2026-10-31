# Utilidades

## `EmaText`: textos que puede guardar el ViewModel

El estado no puede contener un `Context`, así que no puede llamar a `getString`. `EmaText` describe un texto y la vista
lo resuelve.

```kotlin
EmaText.text("Hola")                        // un literal
EmaText.id(R.string.welcome, userName)      // un string de recursos con argumentos
EmaText.plural(R.plurals.friends, count)    // un plural de recursos
EmaText.composition(EmaText.id(R.string.a), EmaText.text(" "), EmaText.id(R.string.b))  // concatenación
EmaText.empty()
```

Resuélvelo donde tengas un contexto:

```kotlin
data.title.stringResource()                 // Compose (com.carmabs.emanate.compose.extension)
data.title.string(context)                  // Android (com.carmabs.emanate.android.extension)
```

`R.string.x.toEmaText(args)` y `R.plurals.x.toEmaText(quantity, args)` son atajos de `emanate-android`.

Dos `EmaText` son iguales cuando describen el mismo texto, argumentos incluidos, así que un estado que los contenga se
compara correctamente. Un texto sin argumentos nunca se formatea: `EmaText.text("50%")` se muestra tal cual.

## `EmaResult<T, E>`

Como `kotlin.Result`, pero el fallo puede ser de **cualquier tipo**, no solo `Throwable`. Úsalo como tipo de retorno de
los casos de uso, para que los fallos formen parte del contrato:

```kotlin
class LoginUseCase(private val repository: Repository) :
    EmaUseCase<LoginUseCase.Input, EmaResult<User, LoginException>>() {

    override suspend fun useCaseFunction(input: Input) =
        repository.login(LoginRequest(input.username, input.password))

    data class Input(val username: String, val password: String)
}
```

```kotlin
result
    .onSuccess { user -> /* ... */ }
    .onFailure { error -> /* ... */ }
```

| Función                                            | Qué hace                                     |
|----------------------------------------------------|----------------------------------------------|
| `EmaResult.success(data)` / `EmaResult.failure(e)` | Crea un resultado.                           |
| `isSuccess` / `isFailure`                          | Indica cuál de los dos es.                   |
| `getOrNull()`, `getOrDefault(x)`, `getOrThrow()`, `getFailureOrNull()` | Lo lee.                  |
| `map`, `flatMap`                                   | Transforman el éxito.                        |
| `mapError`, `flatMapError`                         | Transforman el fallo.                        |
| `onSuccess`, `onFailure`                           | Efectos secundarios; devuelven el mismo resultado. |

## Casos de uso

| Clase                          | Para                                                                             |
|--------------------------------|----------------------------------------------------------------------------------|
| `EmaUseCase<I, O>`             | Una operación `suspend`. Se ejecuta en el dispatcher de segundo plano que recibe en el constructor, por defecto el `io` de la [configuración](configuration.es.md) (`Dispatchers.IO` en Android). |
| `EmaFlowUseCase<I, O>`         | Una operación que emite valores a lo largo del tiempo (`Flow<O>`), recogida en el dispatcher de segundo plano que recibe en el constructor. |
| `EmaSyncUseCase<I, O>`         | Una operación rápida y síncrona.                                                 |

Implementa `useCaseFunction` (la función protegida) y llama al caso de uso como si fuera una función: `loginUseCase(input)`.
Para ejecutarlo una vez en otro dispatcher, pásaselo al llamarlo: `loginUseCase(input, dispatcher)`.

## Imprimir objetos

`toStringPretty()` imprime cualquier objeto, por ejemplo un estado, con el `printer` de la
[configuración](configuration.es.md#imprimir-objetos). Con `EmaConfiguration.Android(debug)` imprime todos los campos,
los objetos anidados y las colecciones, con sangría:

```kotlin
Log.d("Login", state.toStringPretty())
```

Pasa otro printer para imprimir con él, por ejemplo el de tu propia configuración: `state.toStringPretty(printer)`.

## `EmaUtils`

Las funciones de utilidad están agrupadas por temática en `EmaUtils`, así que escribiendo `EmaUtils.` aparecen todas.
Los módulos de Android añaden sus propias funciones a las temáticas, bajo el nombre de la plataforma:

| Temática                     | Módulo                    | Funciones                                                              |
|------------------------------|---------------------------|------------------------------------------------------------------------|
| `EmaUtils.Coroutines`        | `emanate-core`            | `suspendCoroutineWithTimeout(duration) { }`, `requireMinDurationOf(duration) { }` |
| `EmaUtils.Flows`             | `emanate-core`            | `singleEvent<T>()`: un `MutableSharedFlow` sin replay, para señales de un solo uso. |
| `EmaUtils.Positions`         | `emanate-core`            | `iterateFromCenter(index, length)`, `iterateFromEnd(index, length)`    |
| `EmaUtils.Size`              | `emanate-core`            | `fitInTargetMaxSizeProportionally(width, height, targetWidth, targetHeight)` |
| `EmaUtils.Values`            | `emanate-core`            | `checkUpdate(old, new) { }`: ejecuta el bloque solo si el valor ha cambiado. |
| `EmaUtils.Screen.Android`    | `emanate-android`         | `metrics(context, includeInsets)`                                      |
| `EmaUtils.Screen.Compose`    | `emanate-compose-android` | `widthDp()`, `heightDp()`, `widthPx()`, `heightPx()`                   |
| `EmaUtils.Color.Android`     | `emanate-android`         | `fromGradient(colors, positions, value)`, `lerp(colorA, colorB, t)`    |
| `EmaUtils.View.Android`      | `emanate-android-view`    | `visibility(visible, gone)`: `VISIBLE`, `GONE` o `INVISIBLE`.          |
| `EmaUtils.Preview.Compose`   | `emanate-compose-android` | `isInPreview()`, `skipForPreview(previewComposable) { }`               |

```kotlin
val user = EmaUtils.Coroutines.requireMinDurationOf(1.seconds) { loginUseCase(input) }
val metrics = EmaUtils.Screen.Android.metrics(context)
```

Los grupos de plataforma, como `Android` o `Compose`, son propiedades de extensión: el IDE las importa cuando
autocompletas `EmaUtils.Screen.`.

## Otras ayudas

| Ayuda                     | Qué es                                                                          |
|---------------------------|---------------------------------------------------------------------------------|
| `EmaImage`                | Una imagen descrita como `ByteArray`, `Uri` o `Id` de recurso, con tamaño y tinte. |
| `EmaUniqueSelector`       | Mantiene una sola opción seleccionada en un grupo e indica cuáles se han deseleccionado. |

`emanate-core` también tiene extensiones para números, strings, listas, fechas y flows en `com.carmabs.emanate.core.extension`.

## Ayudas de Android

`emanate-android` tiene las ayudas de Android que comparten Compose y las vistas:

| Ayuda                                                      | Qué hace                                                            |
|------------------------------------------------------------|---------------------------------------------------------------------|
| `EmaText.string(context)`, `Int.toEmaText(...)`            | Resuelven y crean [textos](#ematext-textos-que-puede-guardar-el-viewmodel). |
| `Context.showToast(message, duration)` / `EmaSingleToast`  | Un toast que evita solaparse cuando se muestran varios a la vez.    |
| `Int.dp`, `Int.sp`                                         | Unidades.                                                           |
| `R.drawable.x.getDrawable(context)`, `getBitmap`, `getColor`, `getDimension`, `getFont`... | Leen recursos a partir de su id. |
| `Bitmap.resizeCrop`, `resizeFitInside`, `getRoundedCornerBitmap`, `toByteArray`, `ByteArray.toBitmap` | Transforman imágenes. |
| `EmaUriRes.getResourceId(context)`, `onDrawable`, `onString`... | Resuelven un recurso descrito por su nombre.                   |
| `Bundle.getParcelableCompat`, `Intent.getSerializableExtraCompat`... | Leen extras en todas las versiones de Android.           |
| `Intent.setInitializer`, `Activity.getInitializer`          | [Inicializadores](initializers.es.md) en los intents.              |
| `ComponentActivity.addOnBackPressedListener { }`            | Gestiona el botón atrás con un `EmaBackHandlerStrategy`.            |
| `Context.findActivity()`, `findComponentActivity()`         | Encuentran la activity que hay detrás de un contexto.               |
