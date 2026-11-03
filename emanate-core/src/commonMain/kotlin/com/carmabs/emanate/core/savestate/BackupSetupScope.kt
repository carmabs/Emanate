package com.carmabs.emanate.core.savestate

import com.carmabs.emanate.core.source.EmaSourceDsl
import com.carmabs.emanate.core.source.SetupGuard
import com.carmabs.emanate.core.state.EmaState

/**
 * Created by Carlos Mateo Benito on 14/10/2026.
 *
 * Adds the values of an [EmaBackup], created with backupOf, with [add]: the ones already created, alone or joined with
 * `+`, or a new one with its key. Each value is saved every time it changes and, after the death of the process, it is
 * restored in the order they are added.
 *
 * <p>
 * Copyright (c) 2026 by Carmabs. All rights reserved.
 * </p>
 *
 * @author <a href=“mailto:apps.carmabs@gmail.com”>Carlos Mateo Benito</a>
 */
@EmaSourceDsl
class BackupSetupScope<S : EmaState> internal constructor(private val guard: SetupGuard) {

    internal var backupValueList: List<EmaBackupValue<S, *>> = emptyList()
        private set

    /**
     * Joins two saved values already created to add them together with [add].
     */
    operator fun EmaBackupValue<S, *>.plus(other: EmaBackupValue<S, *>): List<EmaBackupValue<S, *>> =
        listOf(this, other)

    /**
     * Adds saved values already created, in the order they are given.
     *
     * @throws IllegalArgumentException if the key of any is already saved
     */
    fun add(vararg savedValues: EmaBackupValue<S, *>) {
        add(savedValues.asList())
    }

    /**
     * Adds saved values already created, in the order of the collection.
     *
     * @throws IllegalArgumentException if the key of any is already saved
     */
    fun add(savedValues: Iterable<EmaBackupValue<S, *>>) {
        guard.checkNotBuilt()
        var added = backupValueList
        savedValues.forEach { savedValue ->
            require(added.none { it.key == savedValue.key }) { "The key ${savedValue.key} is already saved" }
            added = added + savedValue
        }
        backupValueList = added
    }

    /**
     * Saves text with [key].
     *
     * @param key unique in the backup
     * @param save returns the text to save from the state, or null to save nothing
     * @param restore returns the state with the saved text restored
     */
    fun add(key: String, save: S.() -> String?, restore: S.(value: String) -> S) {
        add(savedValueOf(key, save, restore))
    }

    /**
     * Saves a value of type [T] with [key], converted to text with [codec].
     *
     * @param key unique in the backup
     * @param save returns the value to save from the state, or null to save nothing
     * @param restore returns the state with the saved value restored
     */
    fun <T> add(key: String, codec: EmaBackupCodec<T>, save: S.() -> T?, restore: S.(value: T) -> S) {
        add(savedValueOf(key, codec, save, restore))
    }
}
