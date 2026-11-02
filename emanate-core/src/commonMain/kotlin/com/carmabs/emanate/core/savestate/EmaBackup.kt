package com.carmabs.emanate.core.savestate

import com.carmabs.emanate.core.action.EmaAction
import com.carmabs.emanate.core.initializer.EmaInitializer
import com.carmabs.emanate.core.source.EmaSourcePlugin
import com.carmabs.emanate.core.source.SetupGuard
import com.carmabs.emanate.core.source.SourceSetupScope
import com.carmabs.emanate.core.source.middleware.MiddlewarePosition
import com.carmabs.emanate.core.source.middleware.MiddlewareScope
import com.carmabs.emanate.core.source.middleware.middlewareOf
import com.carmabs.emanate.core.source.reducer.reducerOf
import com.carmabs.emanate.core.state.EmaEvent
import com.carmabs.emanate.core.state.EmaState
import com.carmabs.emanate.core.viewmodel.EmaViewModel
import kotlin.concurrent.Volatile
import kotlin.reflect.KClass

/**
 * Created by Carlos Mateo Benito on 15/10/2026.
 *
 * Parts of the state saved so they are restored after the system kills the process of the app. Save only what the
 * user would lose, like the fields of a form, not the whole state: the platform limits the size of the saved values.
 * Create it with backupOf and install it in the setup of a source, before the reducers that react to
 * [EmaRestoreState]: it adds the reducer that restores the values, so the source doesn't know them.
 *
 * It has everything it needs to work: the [store] where the values are saved and the action that restores them. When
 * the first action of type [restoreOn] reaches the reducers, the backup reads the store and restores its values right
 * after that action. Its middleware runs after the ones of the source, wherever it is installed, so it always sees
 * the new state of each action. From then on, it saves each value every time it changes. Before it, nothing is read nor written,
 * so that action, like an initializer, can't replace the saved values with the initial state.
 *
 * <p>
 * Copyright (c) 2026 by Carmabs. All rights reserved.
 * </p>
 *
 * @author <a href=“mailto:apps.carmabs@gmail.com”>Carlos Mateo Benito</a>
 */
class EmaBackup<S : EmaState> internal constructor(
    private val store: EmaStateStore,
    private val restoreOn: KClass<out EmaAction>,
    internal val values: List<EmaBackupValue<S, *>>
) : EmaSourcePlugin<S, EmaRestoreState> {

    /**
     * Restores the values of [EmaRestoreState] that belong to this backup, in the order they were added. A value that
     * can't be decoded isn't restored.
     */
    private val restoreReducer = reducerOf<S, EmaRestoreState>(EmaRestoreState::class) { action ->
        values.fold(state) { restoredState, backupValue ->
            action.values[backupValue.key]?.let { backupValue.restore(restoredState, it) } ?: restoredState
        }
    }

    /**
     * True once the backup is installed in a setup
     */
    @Volatile
    private var installed = false

    /**
     * True once the action of type [restoreOn] has arrived
     */
    @Volatile
    private var started = false

    /**
     * Last text written in the store, or read from it, of each key
     */
    @Volatile
    private var storedValues: Map<String, String?> = emptyMap()

    /**
     * Texts read from the store that haven't been restored yet
     */
    @Volatile
    private var pendingValues: Map<String, String> = emptyMap()

    /**
     * @throws IllegalStateException if the backup is already installed in another source
     * @throws IllegalArgumentException if another backup of the source saves a key of this one in the same store
     */
    override fun install(setup: SourceSetupScope<S, EmaRestoreState, Nothing>) {
        check(!installed) { "An EmaBackup can only be installed in one source: create one for each source" }
        val keys = values.map { it.key }.toSet()
        setup.installedPlugins.filterIsInstance<EmaBackup<*>>()
            .filter { it !== this && it.store == store }
            .flatMap { backup -> backup.values.map { it.key }.filter { it in keys } }
            .takeIf { it.isNotEmpty() }
            ?.let { repeatedKeys -> throw IllegalArgumentException("The backups have repeated keys: $repeatedKeys") }
        installed = true
        setup.reducers { add(restoreReducer) }
        setup.middlewares(MiddlewarePosition.Last) { add(backupMiddleware) }
    }

    /**
     * The last of the chain, so when next returns the reducers of the action have run, wherever the backup is
     * installed. It restores the values after the first action of type [restoreOn] and saves them from then on.
     */
    private val backupMiddleware = middlewareOf<S, EmaRestoreState, EmaAction>(EmaAction::class) { action ->
        next()
        if (!started) {
            if (!restoreOn.isInstance(action)) return@middlewareOf
            restore()
        }
        save(currentState)
    }

    /**
     * Reads the store and dispatches its values right after the action of type [restoreOn], which is being processed.
     */
    private fun MiddlewareScope<S, EmaRestoreState>.restore() {
        started = true
        storedValues = values.associate { it.key to store.read(it.key) }
        // Pending until restored: saving them from the state before the restore would remove them from the store
        pendingValues = storedValues.mapNotNull { (key, value) -> value?.let { key to it } }.toMap()
        pendingValues.takeIf { it.isNotEmpty() }?.let { dispatch(EmaRestoreState(it)) }
        // After the restore, also if a middleware stops it, so the values don't stay pending forever
        enqueue {
            pendingValues = emptyMap()
            save(currentState)
        }
    }

    private fun MiddlewareScope<S, EmaRestoreState>.save(state: S) {
        val pending = pendingValues
        values.forEach { backupValue ->
            val key = backupValue.key
            if (key in pending) return@forEach
            runHandlingErrors({ "save $key" }) {
                val value = backupValue.encode(state)
                if (value != storedValues[key]) {
                    store.write(key, value)
                    storedValues = storedValues + (key to value)
                }
            }
        }
    }

    private fun <T> EmaBackupValue<S, T>.encode(state: S): String? = save(state)?.let(codec::encode)

    private fun <T> EmaBackupValue<S, T>.restore(state: S, value: String): S =
        codec.decode(value)?.let { restore(state, it) } ?: state
}

/**
 * Creates a backup with the values added in [block].
 *
 * @param store where the values are saved
 * @param restoreOn type of the action after which the saved values are restored
 * @throws IllegalArgumentException if a key is added twice
 */
fun <S : EmaState> backupOf(
    store: EmaStateStore,
    restoreOn: KClass<out EmaAction>,
    block: BackupSetupScope<S>.() -> Unit
): EmaBackup<S> {
    val guard = SetupGuard()
    val scope = BackupSetupScope<S>(guard).apply(block)
    guard.build()
    return EmaBackup(store, restoreOn, scope.backupValueList)
}

/**
 * Creates a backup of the state of the view model with the values added in [block].
 *
 * @param store where the values are saved. Receive it in the constructor of the view model: on Android, the platform
 * gives it when it creates the view model.
 * @param restoreOn type of the action after which the saved values are restored. By default the initializer, which
 * the view model always receives when its view is created.
 * @throws IllegalArgumentException if a key is added twice
 */
fun <S : EmaState, A : EmaAction, E : EmaEvent> EmaViewModel<S, A, E>.backupOf(
    store: EmaStateStore,
    restoreOn: KClass<out EmaAction> = EmaInitializer::class,
    block: BackupSetupScope<S>.() -> Unit
): EmaBackup<S> = com.carmabs.emanate.core.savestate.backupOf(store, restoreOn, block)
