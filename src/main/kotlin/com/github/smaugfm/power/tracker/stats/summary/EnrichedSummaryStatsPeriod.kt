package com.github.smaugfm.power.tracker.stats.summary

import com.github.smaugfm.power.tracker.Event
import com.github.smaugfm.power.tracker.SummaryStatsPeriod
import java.time.Duration
import java.time.Instant

data class EnrichedSummaryStatsPeriod(
    val period: SummaryStatsPeriod,
    val start: Instant,
    val end: Instant,
    private val events: List<Event>
) {
    val type = events.first().type

    val hasNoEvents = events.isEmpty()

    val wholeDuration: Duration = Duration.between(start, end)

    fun countEvents(count: (Event) -> Boolean) = events.count(count)

    fun durations(state: Boolean) =
        if (state) durationsOn else durationsOff

    private val eventsSurrounded by lazy {
        events.toMutableList().also { list ->
            val first = list.first()
            if (first.time != start) list.add(
                0, Event(
                    0, !first.state, first.type, first.configId, start
                )
            )
            val curLast = list.last()
            if (curLast.time != end) list.add(
                Event(
                    0, !curLast.state, curLast.type, curLast.configId, end
                )
            )
        }.toList()
    }

    val eventPairsSurrounded by lazy {
        eventsSurrounded.zip(eventsSurrounded.drop(1))
    }

    private val durationsOn by lazy {
        durationsInternal(true)
    }

    private val durationsOff by lazy {
        durationsInternal(false)
    }

    private fun durationsInternal(state: Boolean) =
        eventPairsSurrounded.filter { it.first.state == state }
            .map { Duration.between(it.first.time, it.second.time) }

}
