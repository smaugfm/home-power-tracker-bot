package com.github.smaugfm.power.tracker.stats

import com.github.smaugfm.power.tracker.dto.Event
import com.github.smaugfm.power.tracker.dto.EventType
import com.github.smaugfm.power.tracker.events.EventsService
import kotlinx.coroutines.FlowPreview
import org.springframework.stereotype.Service

@Service
@FlowPreview
class StatsServiceImpl(
    private val service: EventsService
) : StatsService {
    override suspend fun calculateEventStats(event: Event): List<EventStats> {
        return singleEventStats(event) + summaryEventStats(event)
    }

    private suspend fun singleEventStats(event: Event): List<EventStats.Single> {
        val singleEventStats = when (event.type) {
            EventType.POWER -> getLastInverseStats(event)
            EventType.ISP -> if (event.state) getLastInverseStats(event) else getIspDownStats(
                event
            )
        } ?: return emptyList()

        return listOf(singleEventStats)
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

    private fun summaryEventStats(event: Event): List<EventStats.Summary> {
        //TODO
        return emptyList()
    }
}
