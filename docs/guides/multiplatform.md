# Kotlin Multiplatform

`emanate-core` is a Kotlin Multiplatform library. All of it lives in `commonMain`, so states, actions, events,
ViewModels, sources, plugins, use cases, `EmaResult` and `EmaText` are available on every target:

| Platform        | Targets                                                                          |
|-----------------|----------------------------------------------------------------------------------|
| JVM             | `jvm` (also used by Android)                                                     |
| Web             | `js`, `wasmJs`, `wasmWasi`                                                       |
| Apple           | iOS, macOS, watchOS and tvOS, devices and simulators                             |
| Linux / Windows | `linuxX64`, `linuxArm64`, `mingwX64`                                             |
| Android Native  | `androidNativeArm32`, `androidNativeArm64`, `androidNativeX86`, `androidNativeX64` |

## Sharing the presentation layer

Put the ViewModels in the `commonMain` of a multiplatform module that depends on `emanate-core`:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("com.github.carmabs.ema:emanate-core:$emanateVersion")
        }
    }
}
```

On Android they are drawn with [Compose](../compose/screens.md).
Any other platform only needs to collect `stateFlow` and `eventFlow` and send actions with `dispatch`.

## What is not available everywhere

- **Configuration.** `EmaConfiguration.Android(debug)` is in `emanate-android`. On other platforms call `Emanate.init` with an
  `EmaConfiguration` built for them, or use the default one. See [Configuration](configuration.md).
- **Main dispatcher.** The default ViewModel scope uses `Dispatchers.Main`. On platforms without one (Linux, Windows,
  Android Native), configure another `main` in `EmaDispatchers`, in the configuration of Emanate or of the ViewModel.
- **IO dispatcher.** JS and Wasm don't have `Dispatchers.IO`, so use cases run on `Dispatchers.Default` there. See
  [Dispatchers](configuration.md#dispatchers).
