package com.github.smaugfm.power.tracker.stats.summary

import com.github.smaugfm.power.tracker.*
import com.github.smaugfm.power.tracker.stats.EventStats
import com.github.smaugfm.power.tracker.stats.StatsService
import mu.KotlinLogging
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Service
import java.time.Duration
import kotlin.math.roundToLong

private val log = KotlinLogging.logger { }

@Service
@Order(2)
class SummaryStatsService(private val periodEnricher: SummaryStatsPeriodEnricher) : StatsService {
    override suspend fun calculate(event: Event): List<EventStats.Summary> =
        periodEnricher.forEvent(event).mapNotNull { enrichedSummaryStatsPeriod ->
            calculateStats(enrichedSummaryStatsPeriod).also {
                log.info {
                    "Calculated stats for " +
                            "configId=${event.configId}, $enrichedSummaryStatsPeriod, $it"
                }
            }
        }

    suspend fun calculateStats(period: EnrichedSummaryStatsPeriod): EventStats.Summary? {
        if (period.hasNoEvents) return null

        return EventStats.Summary(
            period.type,
            period.period,
            turnOffCount = period.countEvents { !it.state },
            upTotal = period.durations(true).reduce { acc, duration -> acc.plus(duration) },
            downTotal = period.durations(false).reduce { acc, duration -> acc.plus(duration) },
            upPercent = period.durations(true).reduce { acc, duration -> acc.plus(duration) }
                .toMillis().toDouble() / period.wholeDuration.toMillis() * 100,
            upPeriodicStats = periodicStats(period, true),
            downPeriodicStats = periodicStats(period, false)
        )
    }

    private fun periodicStats(period: EnrichedSummaryStatsPeriod, state: Boolean): PeriodicStats {
        val durations = period.durations(state)
        return PeriodicStats(
            state,
            durations.maxOf { it },
            durations.minOf { it },
            Duration.ofMillis(durations.map { it.toMillis() }.median().roundToLong())
        )
    }

}
