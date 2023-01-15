package com.github.smaugfm.power.tracker.stats.image

import com.github.smaugfm.power.tracker.*
import com.github.smaugfm.power.tracker.stats.EventStats
import com.github.smaugfm.power.tracker.stats.EventStats.LastWeekPowerScheduleImage
import com.github.smaugfm.power.tracker.stats.StatsService
import com.github.smaugfm.power.tracker.stats.summary.EnrichedSummaryStatsPeriod
import com.github.smaugfm.power.tracker.stats.summary.SummaryStatsPeriodEnricher
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Service
import java.time.ZoneId

@Order(3)
@Service
class ScheduleImageStatsService(
    private val scheduleImageGenerator: YasnoScheduleImageGenerator,
    private val periodEnricher: SummaryStatsPeriodEnricher
) : StatsService {
    override suspend fun calculate(event: Event): List<EventStats> {
        if (event.type != EventType.POWER) return emptyList()

        return periodEnricher.forEvent(event) {
            it == SummaryStatsPeriod.LastWeek
        }.map {
            getImage(it)
        }
    }

    suspend fun getImage(enrichedPeriod: EnrichedSummaryStatsPeriod): LastWeekPowerScheduleImage {
        val hours = calculateOutageHours(enrichedPeriod)
        val bytes = scheduleImageGenerator.createSchedule(
            YasnoGroup.Group1,
            hours
        )
        return LastWeekPowerScheduleImage(
            bytes
        )
    }

    fun calculateOutageHours(enrichedPeriod: EnrichedSummaryStatsPeriod): List<IntRange> =
        enrichedPeriod
            .eventPairsSurrounded
            .filter { !it.first.state }
            .map {
                val getHour = { event: Event ->
                    val zoned = event.time.atZone(ZoneId.systemDefault())
                    val weekHour = (zoned.dayOfWeek.value - 1) * 24 + zoned.hour
                    weekHour to zoned.minute
                }
                var (weekHourStart, minuteStart) = getHour(it.first)
                var (weekHourEnd, minuteEnd) = getHour(it.second)

                if (weekHourStart != weekHourEnd) {
                    if (weekHourStart + 1 == weekHourEnd) {
                        if (minuteStart > 30 && minuteEnd <= 30) {
                            val firstHourOutageMinutes = 60 - minuteStart
                            val secondHourOutageMinutes = minuteEnd
                            if (firstHourOutageMinutes > secondHourOutageMinutes) {
                                weekHourEnd--
                            } else {
                                weekHourStart++
                            }
                        }
                    }
                    if (minuteStart <= 30 || minuteEnd > 30) {
                        if (minuteStart > 30)
                            weekHourStart++
                        if (minuteEnd <= 30)
                            weekHourEnd--
                    }
                }

                weekHourStart..weekHourEnd
            }
}
