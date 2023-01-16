package com.github.smaugfm.power.tracker.loop

import com.github.smaugfm.power.tracker.EventType
import com.github.smaugfm.power.tracker.SummaryStatsPeriod
import com.github.smaugfm.power.tracker.interaction.UserInteractionService
import com.github.smaugfm.power.tracker.spring.LaunchCoroutineBean
import com.github.smaugfm.power.tracker.stats.image.ScheduleImageStatsService
import com.github.smaugfm.power.tracker.stats.summary.SummaryStatsPeriodEnricher
import com.github.smaugfm.power.tracker.stats.summary.SummaryStatsService
import kotlinx.coroutines.CoroutineScope
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.time.Instant

private val log = KotlinLogging.logger { }

@Component
class StatsLoop(
    private val userInteraction: UserInteractionService,
    private val summaryStatsService: SummaryStatsService,
    private val scheduleImageStatsService: ScheduleImageStatsService,
    private val periodEnricher: SummaryStatsPeriodEnricher
) : LaunchCoroutineBean {
    override suspend fun launch(scope: CoroutineScope) {
        userInteraction.statsFlow().collect { userInteractionData ->
            val period = SummaryStatsPeriod.LastWeek
            val enrichedPeriod = periodEnricher.forPeriod(
                userInteractionData.configId,
                EventType.POWER,
                period,
                periodEnricher.getStartOfPeriod(period, Instant.now()).toInstant()
            )

            log.info {
                "Calculating stats for " +
                        "configId=${userInteractionData.configId}: $enrichedPeriod"
            }
            val stats = summaryStatsService
                .calculateStats(enrichedPeriod) ?: return@collect
            val image = scheduleImageStatsService.getImage(
                userInteractionData.configId,
                enrichedPeriod
            )

            userInteraction.postStats(
                userInteractionData,
                listOfNotNull(stats, image)
            )
        }
    }
}
