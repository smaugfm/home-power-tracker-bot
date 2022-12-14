package com.github.smaugfm.power.tracker.stats

import com.github.smaugfm.power.tracker.*
import com.github.smaugfm.power.tracker.events.EventsService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit
import kotlin.math.roundToLong

@Service
@Order(2)
class SummaryStatsService(private val service: EventsService) : StatsService {
    override suspend fun calculate(event: Event): List<EventStats.Summary> =
        determinePeriodForEvent(event).mapNotNull { (period, to) ->
            calculateForPeriod(event.configId, event.type, to.toInstant(), period)
        }

    suspend fun calculateForPeriod(
        configId: ConfigId,
        type: EventType,
        to: Instant,
        period: SummaryStatsPeriod,
    ): EventStats.Summary? {
        val start = getStartOfLastPeriod(period, to)
        val events = service.getEventsOfTypeBetween(configId, type, start.toInstant(), to)

        return calculateStats(start.toInstant(), to, period, events)
    }

    private suspend fun calculateStats(
        start: Instant,
        to: Instant,
        period: SummaryStatsPeriod,
        eventsFlow: Flow<Event>
    ): EventStats.Summary? {
        val rawEvents = eventsFlow.toList()
        if (rawEvents.isEmpty()) return null

        val events =
            surroundWithOppositeEvents(rawEvents, start, to)
        val zipped = durations(events)
        val upTotal = stateTotal(zipped, true)

        return EventStats.Summary(
            events.first().type,
            period,
            upTotal = upTotal,
            downTotal = stateTotal(zipped, false),
            upPercent = upTotal.toMillis().toDouble() / Duration.between(start, to).toMillis() * 100,
            upPeriodicStats = periodicStats(zipped, true),
            downPeriodicStats = periodicStats(zipped, false)
        )
    }

    private fun periodicStats(zipped: List<Pair<Event, Event>>, state: Boolean): PeriodicStats {
        val durations =
            zipped
                .filter { it.first.state == state }
                .map { Duration.between(it.first.time, it.second.time) }
        return PeriodicStats(
            state,
            durations.maxOf { it },
            durations
                .minOf { it },
            Duration.ofMillis(durations.map { it.toMillis() }.median().roundToLong())
        )
    }

    private fun stateTotal(zipped: List<Pair<Event, Event>>, state: Boolean) =
        zipped
            .fold(Duration.ZERO) { acc, pair ->
                if (pair.first.state == state)
                    acc.plus(Duration.between(pair.first.time, pair.second.time))
                else
                    acc
            }

    private fun durations(events: List<Event>) =
        events.zip(events.drop(1))

    private suspend fun surroundWithOppositeEvents(
        eventsFlow: List<Event>,
        start: Instant,
        to: Instant
    ) = eventsFlow.toMutableList().also { list ->
        val first = list.first()
        if (first.time != start)
            list.add(
                0, Event(
                    0,
                    !first.state,
                    first.type,
                    first.configId,
                    start
                )
            )
        val curLast = list.last()
        if (curLast.time != to)
            list.add(
                Event(
                    0,
                    !curLast.state,
                    curLast.type,
                    curLast.configId,
                    to
                )
            )
    }.toList()

    suspend fun determinePeriodForEvent(event: Event): List<Pair<SummaryStatsPeriod, ZonedDateTime>> {
        val result = mutableListOf<Pair<SummaryStatsPeriod, ZonedDateTime>>()
        val previous = service.findPreviousOfSameType(event)
            ?.time?.atZone(ZoneId.systemDefault()) ?: return result
        val zoned = event.time.atZone(ZoneId.systemDefault())

        val startOfYear = getStartOfLastPeriod(SummaryStatsPeriod.LastYear, event.time)
        if (previous < startOfYear && startOfYear <= zoned) {
            result.add(Pair(SummaryStatsPeriod.LastYear, startOfYear))
        }

        val startOfMonth = getStartOfLastPeriod(SummaryStatsPeriod.LastMonth, event.time)
        if (previous < startOfMonth && startOfMonth <= zoned) {
            result.add(Pair(SummaryStatsPeriod.LastMonth, startOfMonth))
        }

        val startOfWeek = getStartOfLastPeriod(SummaryStatsPeriod.LastWeek, event.time)
        if (previous < startOfWeek && startOfWeek <= zoned) {
            result.add(Pair(SummaryStatsPeriod.LastWeek, startOfWeek))
        }

        return result
    }

    private fun getStartOfLastPeriod(period: SummaryStatsPeriod, to: Instant): ZonedDateTime {
        val zoned = to.atZone(ZoneId.systemDefault())
        return when (period) {
            is SummaryStatsPeriod.Custom -> zoned.minusDays(period.lastDays.toLong())
                .truncatedTo(ChronoUnit.DAYS)

            SummaryStatsPeriod.LastMonth -> zoned.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS)
            SummaryStatsPeriod.LastWeek -> zoned.with(ChronoField.DAY_OF_WEEK, 1).truncatedTo(ChronoUnit.DAYS)
            SummaryStatsPeriod.LastYear -> zoned.withDayOfYear(1).truncatedTo(ChronoUnit.DAYS)
        }
    }
}
