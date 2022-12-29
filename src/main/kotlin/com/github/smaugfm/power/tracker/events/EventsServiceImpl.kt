package com.github.smaugfm.power.tracker.events

import com.github.smaugfm.power.tracker.dto.ConfigId
import com.github.smaugfm.power.tracker.dto.Event
import com.github.smaugfm.power.tracker.dto.EventId
import com.github.smaugfm.power.tracker.dto.EventType
import com.github.smaugfm.power.tracker.dto.PowerIspState
import com.github.smaugfm.power.tracker.persistence.EventEntity
import com.github.smaugfm.power.tracker.persistence.EventsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class EventsServiceImpl(
    private val eventsRepository: EventsRepository,
) : EventsService {

    override suspend fun getAllEvents(configId: ConfigId): Flow<Event> {
        return eventsRepository.findAllByConfigId(configId).asFlow()
            .map { Event(it.id, it.state, it.type, it.configId, it.created) }
    }

    override suspend fun deleteAndGetLaterEvents(eventId: EventId): Flow<Event> {
        TODO("Not yet implemented")
    }

    override fun calculateAddEvents(
        prevState: PowerIspState,
        currentState: PowerIspState
    ): List<Event> {
        TODO("Not yet implemented")
    }

    override suspend fun getCurrentState(configId: ConfigId) =
        supervisorScope {
            val power =
                eventsRepository.findCurrentState(configId, EventType.POWER).awaitFirstOrNull()
            val isp =
                eventsRepository.findCurrentState(configId, EventType.ISP).awaitFirstOrNull()

            PowerIspState(power, isp)
        }
}
