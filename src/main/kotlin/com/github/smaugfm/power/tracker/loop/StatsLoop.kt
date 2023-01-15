package com.github.smaugfm.power.tracker.loop

import com.github.smaugfm.power.tracker.EventType
import com.github.smaugfm.power.tracker.SummaryStatsPeriod
import com.github.smaugfm.power.tracker.interaction.UserInteractionService
import com.github.smaugfm.power.tracker.spring.LaunchCoroutineBean
import com.github.smaugfm.power.tracker.stats.image.ScheduleImageStatsService
import com.github.smaugfm.power.tracker.stats.summary.SummaryStatsPeriodEnricher
import com.github.smaugfm.power.tracker.stats.summary.SummaryStatsService
import kotlinx.coroutines.CoroutineScope
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class StatsLoop(
    private val userInteraction: UserInteractionService,
    private val summaryStatsService: SummaryStatsService,
    private val scheduleImageStatsService: ScheduleImageStatsService,
    private val periodEnricher: SummaryStatsPeriodEnricher
) : LaunchCoroutineBean {
    override suspend fun launch(scope: CoroutineScope) {
        userInteraction.statsFlow().collect { userInteractionData ->
            val enrichedPeriod = periodEnricher.forPeriod(
                userInteractionData.configId,
                EventType.POWER,
                SummaryStatsPeriod.LastWeek,
                periodEnricher.getStartOfPeriod(SummaryStatsPeriod.LastWeek, Instant.now()).toInstant()
            )

            val stats = summaryStatsService
                .calculateStats(enrichedPeriod) ?: return@collect
            val image = scheduleImageStatsService.getImage(enrichedPeriod)

            userInteraction.postStats(
                userInteractionData,
                listOf(stats, image)
            )
        }
    }
}
