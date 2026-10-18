# Texts without Context

The texts of a screen depend on the state: a title with the name of the user, an error message, the number of
friends. But the ViewModel can't resolve them: it doesn't have a `Context`, so it can't call `getString`, and
`emanate-core` is [multiplatform](multiplatform.md), so it doesn't even know what an Android resource is.

`EmaText` solves it: the state holds a **description** of the text, and the view turns it into a `String` when it draws
it.

```kotlin
data class HomeState(
    val title: EmaText = EmaText.empty(),
    val error: EmaText? = null
) : EmaState

reducers {
    add<HomeInitializer.HomeUser> { initializer ->
        state.copy(title = EmaText.id(R.string.home_title, initializer.user.name))
    }
    add<HomeAction.EffectQueryResult.Failed> {
        state.copy(error = EmaText.id(R.string.home_load_error))
    }
}
```

```kotlin
// In a composable
Text(text = state.title.stringResource())
```

The ViewModel decides **which** text is shown, and it is tested without Android: the test checks that the state holds
`EmaText.id(R.string.home_title, "Ana")`, not a translated string.

## Creating a text

| Factory                                        | Describes                                              |
|------------------------------------------------|--------------------------------------------------------|
| `EmaText.text("Hello")`                        | A literal, as it is.                                   |
| `EmaText.text("Hello %s", name)`               | A literal formatted with arguments.                    |
| `EmaText.id(R.string.welcome, userName)`       | A string resource, with or without arguments.          |
| `EmaText.plural(R.plurals.friends, count, count)` | A plural resource: the quantity, then the arguments. |
| `EmaText.composition(text1, text2, ...)`       | Several texts, one after the other.                    |
| `EmaText.empty()`                              | No text. `isEmpty()` tells it.                         |

There are shortcuts too: `"Hello".toEmaText()` in `emanate-core`, and `R.string.welcome.toEmaText(userName)` or
`R.plurals.friends.toEmaText(count, count)` in `emanate-android`.

## Showing a text

| Where            | How                             | Module                     |
|------------------|---------------------------------|----------------------------|
| A composable     | `text.stringResource()`         | `emanate-android-compose`  |
| With a `Context` | `text.string(context)`          | `emanate-android`          |

On other platforms, resolve the `EmaText` the same way: read its type (`Text`, `Id`, `Plural` or `Composition`) and its
arguments, and look the text up in the resources of the platform.

## Equality

Two `EmaText` are equal when they describe the same text, arguments included. So a state that holds them is compared
correctly: setting the same text again doesn't change the state, and a test can check it with `assertState`.

A text without arguments is never formatted, so a `%` in it is safe: `EmaText.text("50%")` is shown as it is.
