package com.github.smaugfm.power.tracker.events

import com.github.smaugfm.power.tracker.dto.ConfigId
import com.github.smaugfm.power.tracker.dto.PowerIspState
import com.github.smaugfm.power.tracker.dto.Event
import com.github.smaugfm.power.tracker.dto.EventId
import kotlinx.coroutines.flow.Flow

interface EventsService {
    suspend fun getAllEvents(configId: ConfigId): Flow<Event>
    suspend fun deleteAndGetLaterEvents(eventId: EventId): Flow<Event>

    suspend fun getCurrentState(configId: ConfigId): PowerIspState

    fun calculateAddEvents(
        prevState: PowerIspState,
        currentState: PowerIspState,
        configId: ConfigId
    ): Flow<Event>
}
