package com.github.smaugfm.power.tracker.interaction

import com.github.smaugfm.power.tracker.dto.ConfigId
import com.github.smaugfm.power.tracker.dto.Event
import com.github.smaugfm.power.tracker.dto.EventId
import kotlinx.coroutines.flow.Flow

interface UserInteractionService {
    suspend fun postEvent(event: Event)
    suspend fun updateEvent(event: Event)
    suspend fun exportEvents(configId: ConfigId, events: Flow<Event>)

    fun deletionFlow(): Flow<EventId>
    fun exportFlow(): Flow<ConfigId>
}