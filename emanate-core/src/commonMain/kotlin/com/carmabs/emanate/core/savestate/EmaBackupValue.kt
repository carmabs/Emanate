package com.carmabs.emanate.core.savestate

import com.carmabs.emanate.core.action.EmaAction
import com.carmabs.emanate.core.state.EmaEvent
import com.carmabs.emanate.core.state.EmaState
import com.carmabs.emanate.core.viewmodel.EmaViewModel

/**
 * Created by Carlos Mateo Benito on 14/10/2026.
 *
 * Part of the state that a view model saves with [key], so it is restored after the system kills the process of the
 * app. Inside backupOf, create it with add, or add one already created. Outside of it, create it with
 * savedValueOf, or implement it as a class, for example to receive it in the constructor of the view model and
 * replace it in a test.
 *
 * <p>
 * Copyright (c) 2026 by Carmabs. All rights reserved.
 * </p>
 *
 * @author <a href=“mailto:apps.carmabs@gmail.com”>Carlos Mateo Benito</a>
 */
interface EmaBackupValue<S : EmaState, T> {

    /**
     * Identifies the value. It must be unique in the backups of the source.
     */
    val key: String

    /**
     * Converts the value to the text kept by the store, and back.
     */
    val codec: EmaBackupCodec<T>

    /**
     * Returns the value to save from [state], or null to save nothing. It is called with every new state, and the
     * value is written only when it changes, so keep it cheap: a small projection of the state.
     */
    fun save(state: S): T?

    /**
     * Returns [state] with the saved [value] restored.
     */
    fun restore(state: S, value: T): S
}

/**
 * Creates a saved value of text.
 *
 * @param save returns the text to save from the state, or null to save nothing
 * @param restore returns the state with the saved text restored
 */
fun <S : EmaState> savedValueOf(
    key: String,
    save: S.() -> String?,
    restore: S.(value: String) -> S
): EmaBackupValue<S, String> = savedValueOf(key, EmaBackupCodec.Text, save, restore)

/**
 * Creates a saved value of type [T], converted to text with [codec].
 *
 * @param save returns the value to save from the state, or null to save nothing
 * @param restore returns the state with the saved value restored
 */
fun <S : EmaState, T> savedValueOf(
    key: String,
    codec: EmaBackupCodec<T>,
    save: S.() -> T?,
    restore: S.(value: T) -> S
): EmaBackupValue<S, T> = object : EmaBackupValue<S, T> {

    override val key: String = key

    override val codec: EmaBackupCodec<T> = codec

    override fun save(state: S): T? = state.save()

    override fun restore(state: S, value: T): S = state.restore(value)
}

fun <S : EmaState, A : EmaAction, E : EmaEvent> EmaViewModel<S, A, E>.savedValueOf(
    key: String,
    save: S.() -> String?,
    restore: S.(value: String) -> S
): EmaBackupValue<S, String> = com.carmabs.emanate.core.savestate.savedValueOf(key, save, restore)

fun <S : EmaState, A : EmaAction, E : EmaEvent, T> EmaViewModel<S, A, E>.savedValueOf(
    key: String,
    codec: EmaBackupCodec<T>,
    save: S.() -> T?,
    restore: S.(value: T) -> S
): EmaBackupValue<S, T> = com.carmabs.emanate.core.savestate.savedValueOf(key, codec, save, restore)
