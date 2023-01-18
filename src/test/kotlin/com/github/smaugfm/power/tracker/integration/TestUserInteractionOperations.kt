package com.github.smaugfm.power.tracker.integration

import com.github.smaugfm.power.tracker.Event
import com.github.smaugfm.power.tracker.EventDeletionRequest
import com.github.smaugfm.power.tracker.UserInteractionData
import com.github.smaugfm.power.tracker.interaction.UserInteractionOperations
import com.github.smaugfm.power.tracker.stats.EventStats
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.emptyFlow
import mu.KotlinLogging
import java.time.Duration

val log = KotlinLogging.logger { }

class TestUserInteractionOperations : UserInteractionOperations<UserInteractionData> {
    override suspend fun postForEvent(event: Event, stats: List<EventStats>) {
        log.info { "EVENT: $event, stats: $stats" }
    }

    val deletionChannel = Channel<EventDeletionRequest<UserInteractionData>>()

    override suspend fun updateForEvent(event: Event, stats: List<EventStats>) {
        log.info { "UPDATE FOR EVENT: $event, stats: $stats" }
    }

    override suspend fun deleteForEvent(event: Event) {
        log.info { "DELETE FOR EVENT: $event" }
    }

    override suspend fun postNoStats(data: UserInteractionData) {
        TODO("Not yet implemented")
    }

    override suspend fun postStats(data: UserInteractionData, stats: List<EventStats>) {
        TODO("Not yet implemented")
    }

    override suspend fun postExport(data: UserInteractionData, events: Flow<Event>) {
        TODO("Not yet implemented")
    }

    override suspend fun postUnstableNetworkTimeout(duration: Duration) {
        log.info { "NETWORK UNSTABLE for $duration" }
    }

    override fun deletionFlow(): Flow<EventDeletionRequest<UserInteractionData>> =
        deletionChannel.consumeAsFlow()

    override fun exportFlow(): Flow<UserInteractionData> {
        return emptyFlow()
    }

    override fun statsFlow(): Flow<UserInteractionData> {
        TODO("Not yet implemented")
    }

}
