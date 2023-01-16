package com.github.smaugfm.power.tracker.stats.summary

import com.github.smaugfm.power.tracker.ConfigId
import com.github.smaugfm.power.tracker.Event
import com.github.smaugfm.power.tracker.EventType
import com.github.smaugfm.power.tracker.SummaryStatsPeriod
import com.github.smaugfm.power.tracker.events.EventsService
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit

@Component
class SummaryStatsPeriodEnricher(private val service: EventsService) {

    suspend fun forEvent(
        event: Event,
        periodFilter: (SummaryStatsPeriod) -> Boolean = { true }
    ): List<EnrichedSummaryStatsPeriod> =
        determinePeriodForEvent(event)
            .filter { periodFilter(it) }
            .map { period ->
                val startOfPeriod = getStartOfPeriod(period, event.time)

                forPeriod(event.configId, event.type, period, startOfPeriod.toInstant())
            }

    suspend fun forPeriod(
        configId: ConfigId,
        type: EventType,
        period: SummaryStatsPeriod,
        until: Instant = Instant.now()
    ): EnrichedSummaryStatsPeriod {
        val start = getStartOfPeriod(period, until)
        val events = service
            .getEventsOfTypeBetween(configId, type, start.toInstant(), until)
            .toList()

        return EnrichedSummaryStatsPeriod(
            period,
            start.toInstant(),
            until,
            events
        )
    }

    suspend fun determinePeriodForEvent(event: Event): List<SummaryStatsPeriod> {
        val result = mutableListOf<SummaryStatsPeriod>()
        val previous = service.findPreviousOfSameType(event)
            ?.time?.atZone(ZoneId.systemDefault()) ?: return result
        val zoned = event.time.atZone(ZoneId.systemDefault())

        val startOfYear = getStartOfPeriod(SummaryStatsPeriod.LastYear, event.time)
        if (previous < startOfYear && startOfYear <= zoned) {
            result.add(SummaryStatsPeriod.LastYear)
        }

        val startOfMonth = getStartOfPeriod(SummaryStatsPeriod.LastMonth, event.time)
        if (previous < startOfMonth && startOfMonth <= zoned) {
            result.add(SummaryStatsPeriod.LastMonth)
        }

        val startOfWeek = getStartOfPeriod(SummaryStatsPeriod.LastWeek, event.time)
        if (previous < startOfWeek && startOfWeek <= zoned) {
            result.add(SummaryStatsPeriod.LastWeek)
        }

        return result
    }

    fun getStartOfPeriod(period: SummaryStatsPeriod, to: Instant): ZonedDateTime {
        val zoned = to.atZone(ZoneId.systemDefault())
        val result = when (period) {
            is SummaryStatsPeriod.Custom -> zoned.minusDays(period.lastDays.toLong())
                .truncatedTo(ChronoUnit.DAYS)

            SummaryStatsPeriod.LastMonth -> zoned.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS)
            SummaryStatsPeriod.LastWeek -> zoned.with(ChronoField.DAY_OF_WEEK, 1).truncatedTo(ChronoUnit.DAYS)
            SummaryStatsPeriod.LastYear -> zoned.withDayOfYear(1).truncatedTo(ChronoUnit.DAYS)
        }
        return if (result == zoned)
            when (period) {
                is SummaryStatsPeriod.Custom -> result
                SummaryStatsPeriod.LastMonth -> result.minusMonths(1)
                SummaryStatsPeriod.LastWeek -> result.minusWeeks(1)
                SummaryStatsPeriod.LastYear -> result.minusYears(1)
            }
        else result
    }
}
