package com.github.smaugfm.power.tracker.events

import com.github.smaugfm.power.tracker.*
import com.github.smaugfm.power.tracker.persistence.EventEntity
import com.github.smaugfm.power.tracker.persistence.EventsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingleOrNull
import mu.KotlinLogging
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import java.time.Instant

private val log = KotlinLogging.logger { }

@Service
class EventsServiceImpl(
    private val eventsRepository: EventsRepository
) : EventsService {

    override suspend fun findAllEvents(configId: ConfigId): Flow<Event> =
        eventsRepository.findAllByConfigIdOrderByCreatedDesc(configId)
            .mapFluxDto()

    override suspend fun getEvent(eventId: EventId): Event? =
        eventsRepository.findById(eventId).awaitSingleOrNull()?.let(::mapDto)

    override suspend fun findPreviousLike(
        event: Event,
        state: Boolean?,
        type: EventType?,
    ): Event? =
        eventsRepository.findFirstPreviousLike(event.configId, event.time, state, type)
            .awaitFirstOrNull()
            ?.let(this::mapDto)

    override suspend fun getEventsAfter(configId: ConfigId, time: Instant): Flow<Event> =
        eventsRepository.findAllByConfigIdAndCreatedIsGreaterThanEqualOrderByCreatedAsc(
            configId,
            time
        ).mapFluxDto()

    override suspend fun getEventsOfTypeBetween(
        configId: ConfigId,
        type: EventType,
        from: Instant,
        to: Instant
    ): Flow<Event> =
        eventsRepository.findAllByConfigIdAndTypeAndCreatedBetweenOrderByCreatedAsc(
            configId,
            type,
            from,
            to
        ).mapFluxDto()

    override suspend fun deleteEvent(eventId: EventId) {
        log.info { "Deleting eventId=$eventId" }
        eventsRepository.deleteById(eventId).awaitSingleOrNull()
    }

    override fun calculateAddEvents(
        prevState: PowerIspState,
        currentState: PowerIspState,
        configId: ConfigId,
    ): Flow<Event> {
        val events = calculateEvents(prevState, currentState, configId)
        log.info { "Adding new events: $events" }
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

        return PowerIspState(power, isp)
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
