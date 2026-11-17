package com.carmabs.emanate.compose.nav2

import androidx.compose.runtime.Composable
import androidx.savedstate.SavedState
import androidx.savedstate.read
import com.carmabs.emanate.core.initializer.EmaInitializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

/**
 * Created by Carlos Mateo Benito on 20/3/24.
 *
 * <p>
 * Copyright (c) 2024 by Carmabs. All rights reserved.
 * </p>
 *
 * Where a destination of Compose navigation reads its initializer from. The initializer of the route always wins:
 * the destination reads it first and only when the route has none, [readBackup] is used. To not use the backup,
 * navigate with an initializer of the screen without values.
 *
 * The initializer travels in the route as JSON, so it is read with kotlinx.serialization.
 *
 * Create it with the function that matches the origin of the initializer: [fromRoute] or [fromInitializer]. For
 * an initializer that comes from the platform, like the intent of an Android activity, read it there and pass it to
 * [fromInitializer]. To read it from any other place, extend this class and override [readBackup].
 *
 * @author <a href=“mailto:apps.carmabs@gmail.com”>Carlos Mateo Benito</a>
 */
open class EmaInitializerSupport(val serializer: KSerializer<out EmaInitializer>) {

    /**
     * Returns the initializer of the destination: the one of its [routeArguments] and, if they have none, the backup.
     */
    @Composable
    fun read(routeArguments: SavedState?): EmaInitializer? =
        routeArguments?.read { if (contains(EmaInitializer.KEY)) getStringOrNull(EmaInitializer.KEY) else null }
            ?.let { Json.decodeFromString(serializer, it) }
            ?: readBackup()

    /**
     * The initializer used when the route has none. By default, there is none.
     */
    @Composable
    protected open fun readBackup(): EmaInitializer? = null

    companion object {
        /**
         * The initializer comes only from the route of the destination, as the one of [navigate][androidx.navigation.NavController.navigate]
         * with an initializer. It is the one of most screens.
         */
        fun <I : EmaInitializer> fromRoute(serializer: KSerializer<I>) = EmaInitializerSupport(serializer)

        /**
         * When the route has none, the destination uses [initializer], for example the one built from a deep link.
         */
        fun <I : EmaInitializer> fromInitializer(serializer: KSerializer<I>, initializer: I?): EmaInitializerSupport =
            object : EmaInitializerSupport(serializer) {
                @Composable
                override fun readBackup(): EmaInitializer? = initializer
            }
    }
}
