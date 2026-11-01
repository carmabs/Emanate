package com.carmabs.emanate.compose.initializer

import android.content.Context
import android.os.Bundle
import com.carmabs.emanate.android.extension.findActivity
import com.carmabs.emanate.android.extension.getInitializer
import com.carmabs.emanate.android.initializer.bundle.strategy.BundleSerializerStrategy
import com.carmabs.emanate.android.initializer.bundle.strategy.KSerializationBundleStrategy
import com.carmabs.emanate.core.initializer.EmaInitializer
import kotlinx.serialization.KSerializer

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
 * The initializer travels in the route as text, so it is read with kotlinx.serialization: the Parcelable and
 * Serializable strategies only work with Bundles.
 *
 * Create it with the function that matches the origin of the initializer: [fromRoute], [fromIntent], [fromBundle] or
 * [fromInitializer]. To read it from any other place, extend this class and override [readBackup].
 *
 * @author <a href=“mailto:apps.carmabs@gmail.com”>Carlos Mateo Benito</a>
 */
open class EmaInitializerSupport(val serializerStrategy: KSerializationBundleStrategy<*>) {

    /**
     * Returns the initializer of the destination: the one of its [routeArguments] and, if they have none, the backup.
     */
    fun read(routeArguments: Bundle?, context: Context): EmaInitializer? =
        routeArguments?.getInitializer<EmaInitializer>(serializerStrategy) ?: readBackup(context)

    /**
     * The initializer used when the route has none. By default, there is none.
     */
    protected open fun readBackup(context: Context): EmaInitializer? = null

    companion object {
        /**
         * The initializer comes only from the route of the destination, as the one of [navigate][androidx.navigation.NavController.navigate]
         * with an initializer. It is the one of most screens.
         */
        fun <I : EmaInitializer> fromRoute(serializer: KSerializer<I>) =
            EmaInitializerSupport(BundleSerializerStrategy.kSerialization(serializer))

        /**
         * When the route has none, the initializer comes from the intent of the activity. Use it in the first screen
         * of an activity that is opened with an initializer.
         */
        fun <I : EmaInitializer> fromIntent(serializer: KSerializer<I>): EmaInitializerSupport {
            val strategy = BundleSerializerStrategy.kSerialization(serializer)
            return object : EmaInitializerSupport(strategy) {
                override fun readBackup(context: Context): EmaInitializer? =
                    context.findActivity().intent?.extras?.getInitializer<EmaInitializer>(strategy)
            }
        }

        /**
         * When the route has none, the initializer comes from [bundle], for example the arguments of a fragment that
         * contains the navigation graph.
         */
        fun <I : EmaInitializer> fromBundle(serializer: KSerializer<I>, bundle: Bundle?): EmaInitializerSupport {
            val strategy = BundleSerializerStrategy.kSerialization(serializer)
            return object : EmaInitializerSupport(strategy) {
                override fun readBackup(context: Context): EmaInitializer? =
                    bundle?.getInitializer<EmaInitializer>(strategy)
            }
        }

        /**
         * When the route has none, the destination uses [initializer], for example the one built from a deep link.
         */
        fun <I : EmaInitializer> fromInitializer(serializer: KSerializer<I>, initializer: I?): EmaInitializerSupport {
            val strategy = BundleSerializerStrategy.kSerialization(serializer)
            return object : EmaInitializerSupport(strategy) {
                override fun readBackup(context: Context): EmaInitializer? = initializer
            }
        }
    }
}
