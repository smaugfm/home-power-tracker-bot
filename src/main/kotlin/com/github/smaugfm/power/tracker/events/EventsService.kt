package com.github.smaugfm.power.tracker.events

import com.github.smaugfm.power.tracker.dto.PowerIspState
import com.github.smaugfm.power.tracker.dto.Event
import com.github.smaugfm.power.tracker.dto.EventId

interface EventsService {
    suspend fun deleteEvent(eventId: EventId)
    suspend fun updateEvent(event: Event)

    suspend fun getCurrentState(configId: Long): PowerIspState
    fun calculateAddEvents(
        prevState: PowerIspState,
        currentState: PowerIspState
    ): List<Event>
}
