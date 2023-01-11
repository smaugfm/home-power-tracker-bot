package com.github.smaugfm.power.tracker.stats

import com.github.smaugfm.power.tracker.Event
import com.github.smaugfm.power.tracker.EventType
import com.github.smaugfm.power.tracker.events.EventsService
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Service

@Service
@Order(1)
class SingleEventStatsService(private val service: EventsService) : StatsService {

    override suspend fun calculate(event: Event): EventStats.Single? {
        val singleEventStats = when (event.type) {
            EventType.POWER -> getLastInverseStats(event)
            EventType.ISP ->
                if (event.state) getLastInverseStats(event) else getIspDownStats(event)
        } ?: return null

        return singleEventStats
    }

    private suspend fun getIspDownStats(event: Event): EventStats.Single? {
        val lastInverseStats = getLastInverseStats(event) ?: return null
        val currentState = service.getCurrentState(event.configId)
        if (currentState.hasPower == false) {
            val lastPowerDown =
                service.findPreviousLike(event, type = EventType.POWER, state = false)
            val lastPowerUp =
                service.findPreviousLike(event, type = EventType.POWER, state = true)

            if (lastPowerDown != null) {
                if (lastPowerUp != null)
                    return EventStats.Single.IspDownStats(
                        lastUPSCharge = lastPowerDown.since(lastPowerUp),
                        lastUPSOperation = event.since(lastPowerDown),
                        lastInverse = lastInverseStats.lastInverse
                    )
                return EventStats.Single.IspDownStats(
                    lastUPSCharge = null,
                    lastUPSOperation = event.since(lastPowerDown),
                    lastInverseStats.lastInverse
                )
            }
        }
        return lastInverseStats
    }

    private suspend fun getLastInverseStats(event: Event): EventStats.Single.LastInverseOnly? {
        val prev = service.findPreviousOfSameType(event) ?: return null
        return EventStats.Single.LastInverseOnly(event.state, event.type, event.since(prev))
    }
}
