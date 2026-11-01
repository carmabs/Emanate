# Utilidades de Compose

Las ayudas de `emanate-compose-android` están en `com.carmabs.emanate.compose.extension`.

## Textos y recursos

```kotlin
state.title.stringResource()            // EmaText → String
R.string.welcome.toComposeString(name)  // un string de recursos con argumentos
R.drawable.logo.toComposePainter()      // Painter
R.drawable.ic_add.toComposeImageVector()// ImageVector, de un vector drawable
R.color.primary.toComposeColor()        // Color
```

Un `EmaImage` se pinta con `toComposableImage(contentDescription, modifier, ...)`, o se convierte en un `Painter` con
`toComposePainter()`. Acepta los tres tipos de imagen: `Id` de recurso, `Uri` y `ByteArray`.

## Previews

| Ayuda                                         | Qué hace                                                        |
|-----------------------------------------------|-----------------------------------------------------------------|
| `EmaUtils.Preview.Compose.isInPreview()`      | `true` dentro de la preview del IDE.                            |
| `EmaUtils.Preview.Compose.skipForPreview(previewComposable) { }` | Pinta el contenido, o `previewComposable` dentro de una preview. |
| `value.changeForPreview(previewValue)`        | Devuelve `previewValue` dentro de una preview y `value` fuera.  |
| `EmaImmutableActionDispatcher.EMPTY`          | Un dispatcher de acciones que las ignora todas.                 |
| `LoremIpsum(words).generate()`                | Texto de relleno como un único `String`.                        |

## Tamaños

| Ayuda                                           | Qué hace                                         |
|-------------------------------------------------|--------------------------------------------------|
| `EmaUtils.Screen.Compose.widthDp()`, `heightDp()` | El tamaño de la pantalla en `Dp`.              |
| `EmaUtils.Screen.Compose.widthPx()`, `heightPx()` | El tamaño de la pantalla en píxeles.           |
| `10.pxToDp()`, `10.dp.toPx()`, `10.dp.inSp`     | Conversiones de unidades con la densidad actual. |
| `EmaMeasureViewWidth(viewToMeasure) { width, view -> }` | Mide un composable antes de pintar el contenido que depende de su ancho. |

## Modificadores y animaciones

- `Modifier.fade(fadeSide, startFadePercentage)` difumina el contenido hacia un lado: `FadeSide.TOP`, `BOTTOM`, `START` o
  `END`. Útil en contenido con scroll.
- Dentro de `AnimatedVisibility`, `setOnBeforeVisibleListener { }`, `setOnVisibleListener { }` y `setOnHideListener { }`
  ejecutan código cuando la animación de entrada o de salida llega a cada estado.

## Otras

| Ayuda                                           | Qué hace                                                         |
|-------------------------------------------------|------------------------------------------------------------------|
| `LocalContext.activity`                         | La `ComponentActivity` que contiene el composable.               |
| `list.toImmutable()`                            | `EmaImmutableList`, una lista que Compose trata como estable.    |
| `viewModel.asActionDispatcher<A>()`             | El ViewModel como un `EmaActionDispatcher<A>`.                   |
| `viewModel.asViewModelAction<S, A, E>()`        | El ViewModel como un `EmaViewModelAction<S, A, E>`.              |
| `ScreenContent::class.routeId`                  | La ruta de una pantalla. Consulta [Navegación](navigation.es.md#rutas). |
