package com.carmabs.emanate.sample.androidutils

import android.content.SharedPreferences
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

/**
 * Reads the value saved as JSON under [key], or [default] when there is none or it can't be read.
 *
 * This is the kind of code that goes in a module of Android utilities: it depends on the Android framework
 * ([SharedPreferences]) but not on the domain of any feature, so the modules that store data (data) and the ones that
 * keep small preferences (ui) share it without writing the JSON handling again.
 */
fun <T> SharedPreferences.getJson(key: String, serializer: KSerializer<T>, default: T): T {
    val saved = getString(key, null) ?: return default
    return runCatching { Json.decodeFromString(serializer, saved) }.getOrDefault(default)
}

/**
 * Saves [value] as JSON under [key]. It is applied in the background, as [SharedPreferences.Editor.apply] does.
 */
fun <T> SharedPreferences.putJson(key: String, serializer: KSerializer<T>, value: T) {
    edit().putString(key, Json.encodeToString(serializer, value)).apply()
}
