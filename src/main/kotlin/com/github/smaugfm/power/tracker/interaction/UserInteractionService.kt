package com.github.smaugfm.power.tracker.interaction

import com.github.smaugfm.power.tracker.dto.ConfigId
import com.github.smaugfm.power.tracker.dto.Event
import com.github.smaugfm.power.tracker.dto.EventId
import kotlinx.coroutines.flow.Flow
import java.time.Duration

interface UserInteractionService {
    suspend fun postForEvent(event: Event)
    suspend fun deleteForEvent(event: Event)
    suspend fun updateForEvent(event: Event)
    suspend fun exportEvents(configId: ConfigId, events: Flow<Event>)

    suspend fun postUnstableNetworkTimeout(duration: Duration)

    fun deletionFlow(): Flow<EventId>
    fun exportFlow(): Flow<ConfigId>
}
