package com.github.smaugfm.power.tracker.events

import com.github.smaugfm.power.tracker.*
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface EventsService {
    suspend fun getEvent(eventId: EventId): Event?
    suspend fun deleteEvent(eventId: EventId)
    suspend fun getEventsAfter(configId: ConfigId, time: Instant): Flow<Event>
    suspend fun getLastN(number: Int): Flow<Event>
    suspend fun getEventsOfTypeBetween(
        configId: ConfigId,
        type: EventType,
        from: Instant,
        to: Instant
    ): Flow<Event>

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
