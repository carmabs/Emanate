@file:OptIn(ExperimentalAtomicApi::class)

package com.carmabs.emanate.core.action

import com.carmabs.emanate.core.state.EmaEvent
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Created by Carlos Mateo Benito on 03/08/2026.
 *
 * Keeps the events published by a view model or a source until the view consumes them.
 *
 * <p>
 * Copyright (c) 2026 by Carmabs. All rights reserved.
 * </p>
 *
 * @author <a href=“mailto:apps.carmabs@gmail.com”>Carlos Mateo Benito</a>
 */
internal class EventDispatcher<E : EmaEvent> : EmaEventConsumer<E> {

    /**
     * Every published event gets a unique id, so an event equal to one already delivered is still a new event
     */
    private data class PendingEvent<E>(val id: Long, val event: E)

    private val nextId = AtomicLong(0)

    private val pendingEvents = MutableStateFlow<List<PendingEvent<E>>>(emptyList())

    override fun consumeEvent(event: E) {
        pendingEvents.update { pending ->
            val index = pending.indexOfFirst { it.event == event }
            if (index < 0) pending else pending.toMutableList().apply { removeAt(index) }
        }
    }

    /**
     * Dispatches an event to be observed by the view.
     * @param event The event to be dispatched.
     * @param allowDuplicated If true, allows the same event to be dispatched multiple times before being consumed.
     */
    fun publish(event: E, allowDuplicated: Boolean) {
        val id = nextId.addAndFetch(1)
        pendingEvents.update { pending ->
            if (!allowDuplicated && pending.any { it.event == event }) {
                pending
            } else {
                pending + PendingEvent(id, event)
            }
        }
    }

    // Emits only when new events are published, not when pending ones are consumed
    override val eventFlow: Flow<List<E>> =
        pendingEvents
            .distinctUntilChanged { old, new -> old.containsAll(new) }
            .filter { it.isNotEmpty() }
            .map { pending -> pending.map { it.event } }
}
