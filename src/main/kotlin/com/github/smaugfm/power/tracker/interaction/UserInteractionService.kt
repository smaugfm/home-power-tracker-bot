package com.github.smaugfm.power.tracker.interaction

import com.github.smaugfm.power.tracker.*
import com.github.smaugfm.power.tracker.stats.EventStats
import kotlinx.coroutines.flow.Flow
import java.time.Duration

interface UserInteractionService {
    suspend fun postForEvent(event: Event)
    suspend fun deleteForEvent(event: Event)
    suspend fun updateForEvent(event: Event)
    suspend fun exportEvents(data: UserInteractionData, events: Flow<Event>)
    suspend fun postStats(data: UserInteractionData, stats: EventStats.Summary)
    suspend fun postUnstableNetworkTimeout(duration: Duration)

    fun deletionFlow(): Flow<EventDeletionRequest<*>>
    fun exportFlow(): Flow<UserInteractionData>
    fun statsFlow(): Flow<UserInteractionData>
}
