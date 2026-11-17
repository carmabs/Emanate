package com.carmabs.emanate.compose.nav2

import androidx.navigation.NavType
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.write
import com.carmabs.emanate.core.initializer.EmaInitializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

/**
 * Created by Carlos Mateo Benito on 19/3/24.
 *
 * <p>
 * Copyright (c) 2024 by Carmabs. All rights reserved.
 * </p>
 *
 * Navigation type of the initializer: it travels in the route as JSON text and in the arguments as a string.
 *
 * @author <a href=“mailto:apps.carmabs@gmail.com”>Carlos Mateo Benito</a>
 */
internal class EmaInitializerNavType(private val serializer: KSerializer<out EmaInitializer>) :
    NavType<EmaInitializer>(isNullableAllowed = true) {

    override fun get(bundle: SavedState, key: String): EmaInitializer? =
        bundle.read { if (contains(key)) getStringOrNull(key) else null }?.let(::parseValue)

    override fun parseValue(value: String): EmaInitializer = Json.decodeFromString(serializer, value)

    override fun serializeAsValue(value: EmaInitializer): String = encodeInitializer(serializer, value)

    override fun put(bundle: SavedState, key: String, value: EmaInitializer) {
        bundle.write { putString(key, serializeAsValue(value)) }
    }
}

@Suppress("UNCHECKED_CAST")
internal fun encodeInitializer(serializer: KSerializer<out EmaInitializer>, initializer: EmaInitializer): String =
    Json.encodeToString(serializer as KSerializer<EmaInitializer>, initializer)
