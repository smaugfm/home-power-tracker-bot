package com.github.smaugfm.power.tracker.events

import com.github.smaugfm.power.tracker.dto.Event
import com.github.smaugfm.power.tracker.dto.EventId
import com.github.smaugfm.power.tracker.dto.EventType
import com.github.smaugfm.power.tracker.dto.PowerIspState
import com.github.smaugfm.power.tracker.persistence.EventsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import org.springframework.stereotype.Service

@Service
class EventsServiceImpl(
    private val eventsRepository: EventsRepository,
) : EventsService {
    override suspend fun deleteEvent(eventId: EventId) {
        TODO("Not yet implemented")
    }

    override suspend fun updateEvent(event: Event) {
        TODO("Not yet implemented")
    }

    override suspend fun getCurrentState(configId: Long) =
        supervisorScope {
            val power = async(Dispatchers.IO) {
                eventsRepository.findCurrentState(configId, EventType.POWER)
            }
            val isp = async(Dispatchers.IO) {
                eventsRepository.findCurrentState(configId, EventType.ISP)
            }

            PowerIspState(power.await(), isp.await())
        }

    override fun calculateAddEvents(
        prevState: PowerIspState,
        currentState: PowerIspState
    ): List<Event> {
        TODO("Not yet implemented")
    }
}
