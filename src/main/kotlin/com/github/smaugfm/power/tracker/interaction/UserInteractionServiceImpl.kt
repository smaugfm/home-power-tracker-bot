package com.github.smaugfm.power.tracker.interaction

import com.github.smaugfm.power.tracker.dto.ConfigId
import com.github.smaugfm.power.tracker.dto.Event
import com.github.smaugfm.power.tracker.dto.EventId
import com.github.smaugfm.power.tracker.stats.StatsService
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapMerge
import org.springframework.stereotype.Service

@Service
@FlowPreview
class UserInteractionServiceImpl(
    private val statsService: StatsService,
    private val operations: List<UserInteractionOperations>
) : UserInteractionService {
    override suspend fun postEvent(event: Event) {
        val stats = statsService.calculateEventStats(event)
        operations.forEach {
            stats.collect { stat ->
                it.postEvent(event, stat)
            }
        }
    }

    override suspend fun updateEvent(event: Event) {
        val stats = statsService.calculateEventStats(event)
        operations.forEach {
            stats.collect { stat ->
                it.updateEvent(event, stat)
            }
        }
    }

    override suspend fun exportEvents(configId: ConfigId, events: Flow<Event>) {
        operations.forEach {
            it.postExport(configId, events)
        }
    }

    override fun deletionFlow(): Flow<EventId> {
        return operations.asFlow().flatMapMerge { it.deletionFlow() }
    }

    override fun exportFlow(): Flow<ConfigId> {
        return operations.asFlow().flatMapMerge { it.exportFlow() }
    }
}
