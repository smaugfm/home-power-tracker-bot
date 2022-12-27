package com.github.smaugfm.power.tracker.interaction

import com.github.smaugfm.power.tracker.events.Event
import com.github.smaugfm.power.tracker.events.EventId
import com.github.smaugfm.power.tracker.events.NewEvent
import kotlinx.coroutines.flow.Flow

interface UserInteractionService {
    suspend fun postEvent(event: Event)
    suspend fun updateEvent(event: Event)

    fun deletionFlow(): Flow<EventId>
    fun exportFlow(): Flow<(events: Flow<Event>) -> Unit>
}
