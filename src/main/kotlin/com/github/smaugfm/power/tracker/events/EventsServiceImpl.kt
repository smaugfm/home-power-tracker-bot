package com.github.smaugfm.power.tracker.events

import com.github.smaugfm.power.tracker.*
import com.github.smaugfm.power.tracker.persistence.EventEntity
import com.github.smaugfm.power.tracker.persistence.EventsRepository
import com.github.smaugfm.power.tracker.persistence.InitialEventEntity
import com.github.smaugfm.power.tracker.persistence.InitialEventsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitLast
import kotlinx.coroutines.reactor.awaitSingleOrNull
import mu.KotlinLogging
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import java.time.Instant

private val log = KotlinLogging.logger { }

@Service
class EventsServiceImpl(
    private val eventsRepository: EventsRepository,
    private val initialEventsRepository: InitialEventsRepository,
) : EventsService {

    override suspend fun findAllEvents(configId: ConfigId): Flow<Event> =
        eventsRepository.findAllByConfigIdOrderByCreatedAsc(configId)
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

    override fun calculateNewEvents(
        prevState: PowerIspState,
        currentState: PowerIspState,
        configId: ConfigId,
    ): List<NewEvent> {
        return listOfNotNull(
            if (prevState.hasPower != currentState.hasPower) {
                if (prevState.hasPower == null)
                    NewEvent.Initial(
                        currentState.hasPower!!,
                        EventType.POWER,
                        configId
                    )
                else
                    NewEvent.Common(
                        currentState.hasPower!!,
                        EventType.POWER,
                        configId
                    )
            } else null,
            if (prevState.hasIsp != currentState.hasIsp) {
                if (prevState.hasIsp == null)
                    NewEvent.Initial(
                        currentState.hasIsp!!,
                        EventType.ISP,
                        configId
                    )
                else
                    NewEvent.Common(
                        currentState.hasIsp!!,
                        EventType.ISP,
                        configId
                    )
            } else null
        )
    }

    override suspend fun addEvents(events: List<NewEvent>): Flow<Event> {
        val (initial, common) = events.separateInitial()
        log.info { "Adding new events: common events $common, initial events $initial" }

        if (initial.isNotEmpty())
            initialEventsRepository
                .saveAll(initial.map(::mapNewInitialEvent))
                .awaitLast()

        return eventsRepository
            .saveAll(common.map(::mapNewEvent))
            .mapFluxDto()
    }

    override suspend fun getCurrentState(configId: ConfigId): PowerIspState {
        var power =
            eventsRepository.findTop1ByConfigIdAndTypeOrderByCreatedDesc(
                configId,
                EventType.POWER
            ).awaitFirstOrNull()?.state
        var isp =
            eventsRepository.findTop1ByConfigIdAndTypeOrderByCreatedDesc(
                configId,
                EventType.ISP
            ).awaitFirstOrNull()?.state

        if (power == null)
            power = initialEventsRepository.findTop1ByConfigIdAndTypeOrderByCreatedDesc(
                configId,
                EventType.POWER
            ).awaitFirstOrNull()?.state
        if (isp == null)
            isp = initialEventsRepository.findTop1ByConfigIdAndTypeOrderByCreatedDesc(
                configId,
                EventType.ISP
            ).awaitFirstOrNull()?.state

        return PowerIspState(power, isp)
    }

    private fun mapDto(e: EventEntity) = Event(e.id, e.state, e.type, e.configId, e.created)
    private fun mapNewInitialEvent(e: NewEvent.Initial) = InitialEventEntity(e.state, e.type, e.configId)
    private fun mapNewEvent(e: NewEvent.Common) = EventEntity(e.state, e.type, e.configId)

    private fun Flux<EventEntity>.mapFluxDto(): Flow<Event> =
        this.map(this@EventsServiceImpl::mapDto).asFlow()

}
