# Contributing

## Project structure

| Module                    | Contents                                                                                          |
|---------------------------|---------------------------------------------------------------------------------------------------|
| `emanate-core`            | Kotlin Multiplatform. All the code lives in `commonMain`.                                         |
| `emanate-android`         | Android integration: configuration, initializers, permissions and the saved state store.         |
| `emanate-android-compose` | Compose support, published as `emanate-compose`.                                                  |
| `emanate-serialization`   | Kotlin Multiplatform. Saves serializable values of the state with kotlinx.serialization.          |
| `emanate-test`            | JVM utilities to test the ViewModels and the sources, a friend module of `emanate-core`.          |
| `sample`                  | A separate Gradle build with the sample app. It includes the library modules from the repository. |

## Building

```bash
./gradlew assembleDebug
```

### The targets of `emanate-core`

`emanate-core` compiles every multiplatform target by default, which is needed to publish it but slow for daily work.
Choose the targets with the `emanate.targets` property, in your `local.properties` (not versioned) or with
`-Pemanate.targets=...`:

```properties
# local.properties
emanate.targets=jvm,js
```

The valid values are `all`, `jvm`, `js`, `wasm`, `apple`, `linux`, `windows` and `androidNative`, separated by commas.
The JVM target is always compiled, because the Android modules use it.

## Tests

| Module                    | Tests                                     | Command                                                |
|---------------------------|-------------------------------------------|--------------------------------------------------------|
| `emanate-core`            | `commonTest`, run on every enabled target | `./gradlew :emanate-core:allTests`                     |
| `emanate-android`         | Robolectric                               | `./gradlew :emanate-android:testDebugUnitTest`         |
| `emanate-serialization`   | JVM tests                                 | `./gradlew :emanate-serialization:jvmTest`             |
| `emanate-android-compose` | Compose UI tests on Robolectric           | `./gradlew :emanate-android-compose:testDebugUnitTest` |
| `emanate-test`            | JUnit                                     | `./gradlew :emanate-test:test`                         |

The resources that only the tests need live in `src/test/res`, so they are not published.

### Coverage

[Kover](https://github.com/Kotlin/kotlinx-kover) aggregates the coverage of the modules:

```bash
./gradlew koverHtmlReport
```

The report is written to `build/reports/kover/html`. `koverXmlReport` writes the XML version.

## Code format

The code is formatted with [Spotless](https://github.com/diffplug/spotless) and ktlint. The rules are in the
`.editorconfig` of the repository: Android Studio code style and lines of up to 120 characters.

```bash
./gradlew spotlessApply   # format
./gradlew spotlessCheck   # verify
```

Run them from the `sample` folder too, for the sample app. The commits that only format the code are listed in
`.git-blame-ignore-revs`; enable it once with `git config blame.ignoreRevsFile .git-blame-ignore-revs`.

## Documentation

The documentation is the `docs/` folder, plain Markdown that renders on GitHub. Every page is in English (`page.md`)
and in Spanish (`page.es.md`): when you change one, update the other too. To preview it as a website with
[MkDocs](https://www.mkdocs.org/), the Material theme and the language selector:

```bash
python3 -m venv .venv
.venv/bin/pip install -r docs/requirements.txt
.venv/bin/mkdocs serve
```

The site has a version selector, built with [mike](https://github.com/jimporter/mike): `dev` is the documentation of
`develop` and every release has its own version, with `latest` pointing to the last one. They are published
automatically (see below). `mkdocs build --strict` runs in the CI, so broken links fail the pull request.

## Continuous integration and releases

Every pull request to `develop` or `master` must pass the `Spotless`, `Tests` and `Docs` checks before it can be merged.

Releases follow git-flow and are automated with GitHub Actions:

1. Run the **Start release** workflow from `develop` (optionally with the version; by default it is the current one
   without `-SNAPSHOT`). It creates `release/X.Y.Z`, a commit that removes `-SNAPSHOT` and a pull request to `master`.
2. Fix whatever is needed in the `release/X.Y.Z` branch and merge the pull request **with a merge commit**, not squash.
3. The **Finish release** workflow creates the tag and the GitHub release, asks [JitPack](https://jitpack.io) to build
   it, publishes the documentation of the version and opens a pull request `sync/X.Y.Z` to `develop` that starts the
   next `X.(Y+1).0-SNAPSHOT`. Merge it with a merge commit too.

The workflows that open pull requests need a `RELEASE_TOKEN` secret: a personal access token with the `repo` and
`workflow` scopes. With the default token GitHub does not run the CI on those pull requests.
