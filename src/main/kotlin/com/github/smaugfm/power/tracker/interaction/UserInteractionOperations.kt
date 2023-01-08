package com.github.smaugfm.power.tracker.interaction

import com.github.smaugfm.power.tracker.dto.ConfigId
import com.github.smaugfm.power.tracker.dto.Event
import com.github.smaugfm.power.tracker.dto.EventId
import com.github.smaugfm.power.tracker.stats.EventStats
import kotlinx.coroutines.flow.Flow
import java.time.Duration

interface UserInteractionOperations {
    suspend fun postForEvent(event: Event, stats: List<EventStats>)
    suspend fun updateForEvent(event: Event, stats: List<EventStats>)
    suspend fun deleteForEvent(event: Event)
    suspend fun postExport(configId: ConfigId, events: Flow<Event>)
    suspend fun postUnstableNetworkTimeout(duration: Duration)

    fun deletionFlow(): Flow<EventId>
    fun exportFlow(): Flow<ConfigId>
}
