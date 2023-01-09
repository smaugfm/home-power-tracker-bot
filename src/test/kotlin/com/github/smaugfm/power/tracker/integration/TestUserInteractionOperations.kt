package com.github.smaugfm.power.tracker.integration

import com.github.smaugfm.power.tracker.ConfigId
import com.github.smaugfm.power.tracker.Event
import com.github.smaugfm.power.tracker.EventId
import com.github.smaugfm.power.tracker.interaction.UserInteractionOperations
import com.github.smaugfm.power.tracker.stats.EventStats
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.emptyFlow
import mu.KotlinLogging
import java.time.Duration

val log = KotlinLogging.logger { }

class TestUserInteractionOperations : UserInteractionOperations {
    override suspend fun postForEvent(event: Event, stats: List<EventStats>) {
        log.info { "EVENT: $event, stats: $stats" }
    }

    val deletionChannel = Channel<EventId>()

    override suspend fun updateForEvent(event: Event, stats: List<EventStats>) {
        log.info { "UPDATE FOR EVENT: $event, stats: $stats" }
    }

    override suspend fun deleteForEvent(event: Event) {
        log.info { "DELETE FOR EVENT: $event" }
    }

    override suspend fun postExport(configId: ConfigId, events: Flow<Event>) {
        TODO("Not yet implemented")
    }

    override suspend fun postUnstableNetworkTimeout(duration: Duration) {
        log.info { "NETWORK UNSTABLE for $duration" }
    }

    override fun deletionFlow(): Flow<EventId> =
        deletionChannel.consumeAsFlow()

    override fun exportFlow(): Flow<ConfigId> {
        return emptyFlow()
    }

}
