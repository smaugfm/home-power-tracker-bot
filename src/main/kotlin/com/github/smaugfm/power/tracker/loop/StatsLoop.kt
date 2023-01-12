package com.github.smaugfm.power.tracker.loop

import com.github.smaugfm.power.tracker.EventType
import com.github.smaugfm.power.tracker.SummaryStatsPeriod
import com.github.smaugfm.power.tracker.interaction.UserInteractionService
import com.github.smaugfm.power.tracker.spring.LaunchCoroutineBean
import com.github.smaugfm.power.tracker.stats.SummaryStatsService
import kotlinx.coroutines.CoroutineScope
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class StatsLoop(
    private val userInteraction: UserInteractionService,
    private val summaryStatsService: SummaryStatsService,
) : LaunchCoroutineBean {
    override suspend fun launch(scope: CoroutineScope) {
        userInteraction.statsFlow().collect { userInteractionData ->
            val stats = summaryStatsService.calculateForPeriod(
                userInteractionData.configId,
                EventType.POWER,
                Instant.now(),
                SummaryStatsPeriod.Month
            ) ?: return@collect

            userInteraction.postStats(
                userInteractionData,
                stats
            )
        }
    }
}
