package com.github.smaugfm.power.tracker.events

import com.github.smaugfm.power.tracker.dto.ConfigId
import com.github.smaugfm.power.tracker.dto.Event
import com.github.smaugfm.power.tracker.dto.EventId
import com.github.smaugfm.power.tracker.dto.EventType
import com.github.smaugfm.power.tracker.dto.PowerIspState
import com.github.smaugfm.power.tracker.persistence.EventEntity
import com.github.smaugfm.power.tracker.persistence.EventsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.supervisorScope
import org.springframework.stereotype.Service
import org.springframework.transaction.ReactiveTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.transaction.support.DefaultTransactionDefinition
import reactor.core.publisher.Flux

@Service
class EventsServiceImpl(
    private val eventsRepository: EventsRepository,
    private val tm: ReactiveTransactionManager,
) : EventsService {

    override suspend fun getAllEvents(configId: ConfigId): Flow<Event> {
        return eventsRepository.findAllByConfigId(configId)
            .mapFluxDto()
    }

    override suspend fun deleteAndGetLaterEvents(eventId: EventId): Flow<Event> =
        eventsRepository
            .findById(eventId)
            .flatMap {
                eventsRepository
                    .deleteById(it.id)
                    .thenReturn(it)
            }.flatMapMany {
                eventsRepository.findAllByConfigIdAndCreatedIsGreaterThanEqual(
                    it.configId,
                    it.created
                )
            }
            .`as`(TransactionalOperator.create(tm)::transactional)
            .mapFluxDto()

    override fun calculateAddEvents(
        prevState: PowerIspState,
        currentState: PowerIspState,
        configId: ConfigId,
    ): Flow<Event> {
        val events = calculateEvents(prevState, currentState, configId)
        return eventsRepository
            .saveAll(events.map { EventEntity(it.state, it.type, it.configId) })
            .mapFluxDto()
    }

    override suspend fun getCurrentState(configId: ConfigId) =
        supervisorScope {
            val power =
                eventsRepository.findTop1ByConfigIdAndTypeOrderByCreatedDesc(
                    configId,
                    EventType.POWER
                ).awaitFirstOrNull()?.state
            val isp =
                eventsRepository.findTop1ByConfigIdAndTypeOrderByCreatedDesc(
                    configId,
                    EventType.ISP
                ).awaitFirstOrNull()?.state

            PowerIspState(power, isp)
        }

    private fun calculateEvents(
        prevState: PowerIspState,
        currentState: PowerIspState,
        configId: ConfigId
    ): List<NewEvent> {
        return listOfNotNull(
            if (prevState.hasPower != currentState.hasPower) NewEvent(
                currentState.hasPower!!,
                EventType.POWER,
                configId
            ) else null,
            if (prevState.hasIsp != currentState.hasIsp) NewEvent(
                currentState.hasIsp!!,
                EventType.POWER,
                configId
            ) else null
        )
    }

    private fun mapDto(e: EventEntity) = Event(e.id, e.state, e.type, e.configId, e.created)

    private fun Flux<EventEntity>.mapFluxDto(): Flow<Event> =
        this.map(this@EventsServiceImpl::mapDto).asFlow()

    private data class NewEvent(
        val state: Boolean,
        val type: EventType,
        val configId: ConfigId,
    )
}
