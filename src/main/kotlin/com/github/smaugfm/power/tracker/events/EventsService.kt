package com.github.smaugfm.power.tracker.events

import com.github.smaugfm.power.tracker.dto.ConfigId
import com.github.smaugfm.power.tracker.dto.Event
import com.github.smaugfm.power.tracker.dto.EventId
import com.github.smaugfm.power.tracker.dto.EventType
import com.github.smaugfm.power.tracker.dto.PowerIspState
import kotlinx.coroutines.flow.Flow

interface EventsService {
    suspend fun deleteAndGetLaterEvents(eventId: EventId): Flow<Event>
    fun calculateAddEvents(
        prevState: PowerIspState,
        currentState: PowerIspState,
        configId: ConfigId
    ): Flow<Event>

    suspend fun getCurrentState(configId: ConfigId): PowerIspState

    suspend fun findAllEvents(configId: ConfigId): Flow<Event>
    suspend fun findPreviousLike(
        event: Event,
        state: Boolean? = null,
        type: EventType? = null
    ): Event?

    suspend fun findPreviousOfSameType(event: Event): Event? =
        this.findPreviousLike(event, type = event.type)
}
