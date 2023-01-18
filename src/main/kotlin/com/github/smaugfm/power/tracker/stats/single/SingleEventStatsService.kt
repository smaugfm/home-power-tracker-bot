package com.github.smaugfm.power.tracker.stats.single

import com.github.smaugfm.power.tracker.Event
import com.github.smaugfm.power.tracker.EventType
import com.github.smaugfm.power.tracker.events.EventsService
import com.github.smaugfm.power.tracker.stats.EventStats
import com.github.smaugfm.power.tracker.stats.StatsService
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Service

@Service
@Order(1)
class SingleEventStatsService(private val service: EventsService) : StatsService {

    override suspend fun calculate(event: Event): List<EventStats.Single> {
        val prev = service.findPreviousOfSameType(event)
            ?: return listOf(EventStats.Single.First(event.state, event.type))

        val other = getOtherConsecutiveStats(prev, event)
        if (!event.state && event.type == EventType.ISP)
            return listOf(getIspDownStats(other, event))

        return listOf(other)
    }

    private suspend fun getIspDownStats(
        consecutive: EventStats.Single.Consecutive,
        event: Event
    ): EventStats.Single.Consecutive {
        val currentState = service.getCurrentState(event.configId)
        if (currentState.hasPower == false) {
            val lastPowerDown =
                service.findPreviousLike(event, type = EventType.POWER, state = false)
            val lastPowerUp =
                service.findPreviousLike(event, type = EventType.POWER, state = true)

            if (lastPowerDown != null) {
                if (lastPowerUp != null)
                    return EventStats.Single.Consecutive.IspDown(
                        lastUPSCharge = lastPowerDown.since(lastPowerUp),
                        lastUPSOperation = event.since(lastPowerDown),
                        lastInverse = consecutive.lastInverse
                    )
                return EventStats.Single.Consecutive.IspDown(
                    lastUPSCharge = null,
                    lastUPSOperation = event.since(lastPowerDown),
                    consecutive.lastInverse
                )
            }
        }
        return consecutive
    }

    private suspend fun getOtherConsecutiveStats(prev: Event, event: Event) =
        EventStats.Single.Consecutive.Other(event.state, event.type, event.since(prev))
}
