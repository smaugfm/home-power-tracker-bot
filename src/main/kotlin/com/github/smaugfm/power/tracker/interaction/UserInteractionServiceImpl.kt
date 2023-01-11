package com.github.smaugfm.power.tracker.interaction

import com.github.smaugfm.power.tracker.ConfigId
import com.github.smaugfm.power.tracker.Event
import com.github.smaugfm.power.tracker.EventId
import com.github.smaugfm.power.tracker.stats.EventStats
import com.github.smaugfm.power.tracker.stats.StatsService
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapMerge
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.time.Duration

private val log = KotlinLogging.logger { }

@Service
@FlowPreview
class UserInteractionServiceImpl(
    private val statsService: List<StatsService>,
    private val operations: List<UserInteractionOperations>
) : UserInteractionService {
    override suspend fun postForEvent(event: Event) {
        val stats = getStats(event)

        log.info { "Notifying of an event: $event with stats $stats" }
        operations.forEach {
            it.postForEvent(event, stats)
        }
    }

    override suspend fun deleteForEvent(event: Event) {
        log.info { "Deleting notifications for event: $event" }
        operations.forEach {
            it.deleteForEvent(event)
        }
    }

    override suspend fun updateForEvent(event: Event) {
        val stats = getStats(event)
        log.info { "Updating notifications or event $event with new stats $stats" }
        operations.forEach {
            it.updateForEvent(event, stats)
        }
    }

    override suspend fun exportEvents(configId: ConfigId, events: Flow<Event>) {
        operations.forEach {
            it.postExport(configId, events)
        }
    }

    override suspend fun postUnstableNetworkTimeout(duration: Duration) {
        log.info { "Notifying of an unstable network $duration" }
        operations.forEach {
            it.postUnstableNetworkTimeout(duration)
        }
    }

    override fun deletionFlow(): Flow<EventId> =
        operations.asFlow().flatMapMerge { it.deletionFlow() }

    override fun exportFlow(): Flow<ConfigId> =
        operations.asFlow().flatMapMerge { it.exportFlow() }

    private suspend fun getStats(event: Event): List<EventStats> =
        statsService.mapNotNull { it.calculate(event) }
}
