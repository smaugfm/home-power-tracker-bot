package com.github.smaugfm.power.tracker.interaction

import com.github.smaugfm.power.tracker.*
import com.github.smaugfm.power.tracker.stats.EventStats
import kotlinx.coroutines.flow.Flow
import java.time.Duration

interface UserInteractionOperations<T : UserInteractionData> {
    suspend fun postForEvent(event: Event, stats: List<EventStats>)
    suspend fun updateForEvent(event: Event, stats: List<EventStats>)
    suspend fun deleteForEvent(event: Event)
    suspend fun postExport(data: T, events: Flow<Event>)
    suspend fun postStats(data: T, stats: List<EventStats>)
    suspend fun postUnstableNetworkTimeout(duration: Duration)

    fun deletionFlow(): Flow<EventDeletionRequest<T>>
    fun exportFlow(): Flow<T>
    fun statsFlow(): Flow<T>
}
