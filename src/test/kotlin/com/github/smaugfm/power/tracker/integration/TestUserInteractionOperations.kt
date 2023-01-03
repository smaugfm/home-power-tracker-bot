package com.github.smaugfm.power.tracker.integration

import com.github.smaugfm.power.tracker.dto.ConfigId
import com.github.smaugfm.power.tracker.dto.Event
import com.github.smaugfm.power.tracker.dto.EventId
import com.github.smaugfm.power.tracker.interaction.UserInteractionOperations
import com.github.smaugfm.power.tracker.stats.EventStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.time.Duration

val log = KotlinLogging.logger { }

class TestUserInteractionOperations : UserInteractionOperations {
    override suspend fun postEvent(event: Event, stats: List<EventStats>) {
        log.info { "EVENT: $event, stats: $stats" }
    }

    override suspend fun updateEvent(event: Event, stats: List<EventStats>) {
        TODO("Not yet implemented")
    }

    override suspend fun postExport(configId: ConfigId, events: Flow<Event>) {
        TODO("Not yet implemented")
    }

    override suspend fun postUnstableNetworkTimeout(duration: Duration) {
        log.info { "NETWORK UNSTABLE for $duration" }
    }

    override fun deletionFlow(): Flow<EventId> {
        return emptyFlow()
    }

    override fun exportFlow(): Flow<ConfigId> {
        return emptyFlow()
    }

}
