package com.carmabs.emanate.core.action

import com.carmabs.emanate.core.state.EmaEvent
import kotlinx.coroutines.flow.Flow

/**
 * Created by Carlos Mateo Benito on 03/08/2026.
 *
 * The side of the events that the view uses: it observes the pending events and consumes them once handled. The
 * events are published only by the view model or the source that owns them.
 *
 * <p>
 * Copyright (c) 2026 by Carmabs. All rights reserved.
 * </p>
 *
 * @author <a href=“mailto:apps.carmabs@gmail.com”>Carlos Mateo Benito</a>
 */
interface EmaEventConsumer<E : EmaEvent> {

    /**
     * Marks [event] as handled, so it is removed from the pending events
     */
    fun consumeEvent(event: E)

    /**
     * The pending events. It emits when new events are published, not when they are consumed.
     */
    val eventFlow: Flow<List<E>>
}
