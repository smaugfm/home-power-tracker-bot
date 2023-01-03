package com.github.smaugfm.power.tracker.events

import com.github.smaugfm.power.tracker.dto.ConfigId
import com.github.smaugfm.power.tracker.dto.Event
import com.github.smaugfm.power.tracker.dto.EventId
import com.github.smaugfm.power.tracker.dto.EventType
import com.github.smaugfm.power.tracker.dto.PowerIspState
import com.github.smaugfm.power.tracker.persistence.EventEntity
import com.github.smaugfm.power.tracker.persistence.EventsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.ReactiveTransactionManager
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.publisher.Flux

private val log = KotlinLogging.logger { }

@Service
class EventsServiceImpl(
    private val eventsRepository: EventsRepository,
    private val tm: ReactiveTransactionManager,
) : EventsService {

    override suspend fun findAllEvents(configId: ConfigId): Flow<Event> {
        return eventsRepository.findAllByConfigId(configId)
            .mapFluxDto()
    }

    override suspend fun findPreviousLike(
        event: Event,
        state: Boolean?,
        type: EventType?,
    ): Event? =
        eventsRepository.findFirstPreviousLike(event.configId, event.time, state, type)
            .awaitFirstOrNull()
            ?.let(this::mapDto)

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
        log.debug { "Adding new events: $events" }
        return eventsRepository
            .saveAll(events.map { EventEntity(it.state, it.type, it.configId) })
            .mapFluxDto()
    }

    override suspend fun getCurrentState(configId: ConfigId): PowerIspState {
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

        return PowerIspState(power, isp).also {
            log.debug { "configId=$configId current state: $it" }
        }
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
                EventType.ISP,
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
