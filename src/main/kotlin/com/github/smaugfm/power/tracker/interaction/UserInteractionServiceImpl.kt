package com.github.smaugfm.power.tracker.interaction

import com.github.smaugfm.power.tracker.dto.ConfigId
import com.github.smaugfm.power.tracker.dto.Event
import com.github.smaugfm.power.tracker.dto.EventId
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
    private val statsService: StatsService,
    private val operations: List<UserInteractionOperations>
) : UserInteractionService {
    override suspend fun postEvent(event: Event) {
        val stats = statsService.calculateEventStats(event)

        log.debug { "Posting event: $event with stats $stats" }
        operations.forEach {
            it.postEvent(event, stats)
        }
    }

    override suspend fun updateEvent(event: Event) {
        val stats = statsService.calculateEventStats(event)
        operations.forEach {
            it.updateEvent(event, stats)
        }
    }

    override suspend fun exportEvents(configId: ConfigId, events: Flow<Event>) {
        operations.forEach {
            it.postExport(configId, events)
        }
    }

    override suspend fun postUnstableNetworkTimeout(duration: Duration) {
        operations.forEach {
            it.postUnstableNetworkTimeout(duration)
        }
    }

    override fun deletionFlow(): Flow<EventId> {
        return operations.asFlow().flatMapMerge { it.deletionFlow() }
    }

    override fun exportFlow(): Flow<ConfigId> {
        return operations.asFlow().flatMapMerge { it.exportFlow() }
    }
}
