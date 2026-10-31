# Contribuir

## Estructura del proyecto

| Módulo                    | Contenido                                                                                                   |
|---------------------------|-------------------------------------------------------------------------------------------------------------|
| `emanate-core`            | Kotlin Multiplatform. Todo el código está en `commonMain`.                                                  |
| `emanate-android`         | Librería Android compartida por las dos tecnologías de UI.                                                  |
| `emanate-compose-android` | Soporte de Compose.                                                                                         |
| `emanate-android-view`    | Soporte del sistema de vistas de Android.                                                                   |
| `emanate-test`            | Utilidades JVM para testear los ViewModels y las sources, friend module de `emanate-core`.                  |
| `sample`                  | Un build de Gradle independiente con la app de ejemplo. Incluye los módulos de la librería del repositorio. |

## Compilar

```bash
./gradlew assembleDebug
```

### Los targets de `emanate-core`

`emanate-core` compila por defecto todos los targets multiplataforma, algo necesario para publicarlo pero lento para el día
a día. Elige los targets con la propiedad `emanate.targets`, en tu `local.properties` (no versionado) o con
`-Pemanate.targets=...`:

```properties
# local.properties
emanate.targets=jvm,js
```

Los valores válidos son `all`, `jvm`, `js`, `wasm`, `apple`, `linux`, `windows` y `androidNative`, separados por comas.
El target JVM se compila siempre, porque lo usan los módulos de Android.

## Tests

| Módulo                    | Tests                                      | Comando                                                |
|---------------------------|--------------------------------------------|--------------------------------------------------------|
| `emanate-core`            | `commonTest`, en todos los targets activos | `./gradlew :emanate-core:allTests`                     |
| `emanate-android`         | Robolectric                                | `./gradlew :emanate-android:testDebugUnitTest`         |
| `emanate-android-view`    | Robolectric                                | `./gradlew :emanate-android-view:testDebugUnitTest`    |
| `emanate-compose-android` | Tests de UI de Compose sobre Robolectric   | `./gradlew :emanate-compose-android:testDebugUnitTest` |
| `emanate-test`            | JUnit                                      | `./gradlew :emanate-test:test`                         |

Los recursos que solo necesitan los tests están en `src/test/res`, así que no se publican.

### Cobertura

[Kover](https://github.com/Kotlin/kotlinx-kover) agrega la cobertura de los cuatro módulos:

```bash
./gradlew koverHtmlReport
```

El informe se genera en `build/reports/kover/html`. `koverXmlReport` genera la versión XML.

## Formato del código

El código se formatea con [Spotless](https://github.com/diffplug/spotless) y ktlint. Las reglas están en el
`.editorconfig` del repositorio: estilo de código de Android Studio y líneas de hasta 120 caracteres.

```bash
./gradlew spotlessApply   # formatear
./gradlew spotlessCheck   # comprobar
```

Ejecútalos también desde la carpeta `sample`, para la app de ejemplo. Los commits que solo formatean el código están en
`.git-blame-ignore-revs`; actívalo una vez con `git config blame.ignoreRevsFile .git-blame-ignore-revs`.

## Documentación

La documentación es la carpeta `docs/`, Markdown normal que se ve bien en GitHub. Cada página está en inglés
(`page.md`) y en español (`page.es.md`): cuando cambies una, actualiza también la otra. Para verla como una web con
[MkDocs](https://www.mkdocs.org/), el tema Material y el selector de idioma:

```bash
python3 -m venv .venv
.venv/bin/pip install -r docs/requirements.txt
.venv/bin/mkdocs serve
```

La web tiene un selector de versión, hecho con [mike](https://github.com/jimporter/mike): `dev` es la documentación
de `develop` y cada release tiene la suya, con `latest` apuntando a la última. Se publican automáticamente (mira
abajo). `mkdocs build --strict` se ejecuta en el CI, así que un enlace roto hace fallar la pull request.

## Integración continua y releases

Toda pull request a `develop` o `master` debe pasar los checks `Spotless`, `Tests` y `Docs` antes de poder mergearse.

Las releases siguen git-flow y se automatizan con GitHub Actions:

1. Ejecuta el workflow **Start release** desde `develop` (opcionalmente con la versión; por defecto es la actual sin
   `-SNAPSHOT`). Crea `release/X.Y.Z`, un commit que quita `-SNAPSHOT` y una pull request a `master`.
2. Arregla lo que haga falta en la rama `release/X.Y.Z` y mergea la pull request **con merge commit**, no con squash.
3. El workflow **Finish release** crea el tag y la release de GitHub, pide a [JitPack](https://jitpack.io) que la
   compile, publica la documentación de la versión y abre una pull request `sync/X.Y.Z` a `develop` que empieza la
   siguiente `X.(Y+1).0-SNAPSHOT`. Mergéala también con merge commit.

Los workflows que abren pull requests necesitan un secreto `RELEASE_TOKEN`: un token de acceso personal con los
permisos `repo` y `workflow`. Con el token por defecto GitHub no ejecuta el CI en esas pull requests.
