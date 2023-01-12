package com.github.smaugfm.power.tracker.interaction

import com.github.smaugfm.power.tracker.Event
import com.github.smaugfm.power.tracker.UserInteractionData
import com.github.smaugfm.power.tracker.interaction.telegram.TelegramUserInteractionOperations
import com.github.smaugfm.power.tracker.stats.EventStats
import com.github.smaugfm.power.tracker.stats.StatsService
import dev.inmo.tgbotapi.utils.RiskFeature
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapMerge
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.time.Duration

private val log = KotlinLogging.logger { }

@RiskFeature
@Service
@FlowPreview
class UserInteractionServiceImpl(
    private val statsService: List<StatsService>,
    private val operations: List<UserInteractionOperations<*>>
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

    override suspend fun exportEvents(data: UserInteractionData, events: Flow<Event>) {
        log.info { "Exporting events for request=$data" }
        operationsByUserData(data).forEach {
            it.postExport(data, events)
        }
    }

    override suspend fun postStats(data: UserInteractionData, stats: EventStats.Summary) {
        log.info { "Posting stats to request=$data" }
        operationsByUserData(data).forEach {
            it.postStats(data, stats)
        }
    }

    override suspend fun postUnstableNetworkTimeout(duration: Duration) {
        log.info { "Notifying of an unstable network $duration" }
        operations.forEach {
            it.postUnstableNetworkTimeout(duration)
        }
    }

    override fun deletionFlow() =
        mergeFlows { deletionFlow() }

    override fun exportFlow() =
        mergeFlows { exportFlow() }

    override fun statsFlow() =
        mergeFlows { statsFlow() }

    private inline fun <T> mergeFlows(crossinline inner: UserInteractionOperations<*>.() -> Flow<T>) =
        operations.asFlow().flatMapMerge { it.inner() }

    private suspend fun getStats(event: Event): List<EventStats> =
        statsService.flatMap { it.calculate(event) }

    @Suppress("UNCHECKED_CAST", "USELESS_CAST")
    private inline fun <reified T : UserInteractionData> operationsByUserData(
        data: T
    ): List<UserInteractionOperations<T>> =
        when (data as UserInteractionData) {
            is UserInteractionData.TelegramUserInteractionData ->
                operations.filterIsInstance<TelegramUserInteractionOperations>()
                        as List<UserInteractionOperations<T>>

            is UserInteractionData.Noop -> emptyList()
        }
}
