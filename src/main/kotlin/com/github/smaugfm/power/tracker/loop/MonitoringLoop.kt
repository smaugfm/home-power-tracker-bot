package com.github.smaugfm.power.tracker.loop

import com.github.smaugfm.power.tracker.*
import com.github.smaugfm.power.tracker.config.ConfigService
import com.github.smaugfm.power.tracker.events.EventsService
import com.github.smaugfm.power.tracker.interaction.UserInteractionService
import com.github.smaugfm.power.tracker.network.NetworkStabilityService
import com.github.smaugfm.power.tracker.network.PingService
import com.github.smaugfm.power.tracker.spring.LaunchCoroutineBean
import com.github.smaugfm.power.tracker.spring.MainLoopProperties
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import kotlin.time.toKotlinDuration

private val log = KotlinLogging.logger { }

@Component
class MonitoringLoop(
    private val configs: ConfigService,
    private val ping: PingService,
    private val networkStability: NetworkStabilityService,
    private val events: EventsService,
    private val userInteraction: UserInteractionService,
    private val props: MainLoopProperties
) : LaunchCoroutineBean {
    private val prevEventsMap = mutableMapOf<ConfigId, Map<EventType, Event>>()

    override suspend fun launch(scope: CoroutineScope) {
        val stateLogged = mutableMapOf<String, Boolean>()

        while (true) {
            if (!networkStability.waitStable())
                continue

            configs.getAll()
                .map { config ->
                    scope.launch(Dispatchers.IO) {
                        val prevState = events.getCurrentState(config.id)
                        val currentState = ping.ping(this, config)
                        logInitialState(stateLogged, config, prevState, currentState)

                        if (prevState != currentState) {
                            processStateChange(config, prevState, currentState)
                        }
                    }
                }.catch {
                    log.error(it) { "Error in main loop while processing $this" }
                }.collect {
                    it.join()
                }

            delay(props.interval.toKotlinDuration())
        }
    }

    suspend fun processStateChange(
        config: Config,
        prevState: PowerIspState,
        curState: PowerIspState
    ) {
        log.info {
            "State diff for host=${config.address}. " +
                    "prev: $prevState, cur: $curState"
        }
        val newEvents =
            calculateNewEvents(config.id, prevState, curState)

        if (newEvents.isEmpty())
            return

        events
            .addEvents(newEvents)
            .collect { event ->
                prevEventsMap.compute(event.configId) { _, prev ->
                    prev?.toMutableMap()?.also {
                        it[event.type] = event
                    }?.toMap() ?: mapOf(event.type to event)
                }
                userInteraction.postForEvent(event)
            }
    }

    suspend fun calculateNewEvents(
        configId: ConfigId,
        prevState: PowerIspState,
        curState: PowerIspState
    ): List<NewEvent> {
        val prevEvents = prevEventsMap[configId] ?: mutableMapOf()
        val newEvents =
            events
                .calculateNewEvents(prevState, curState, configId)
                .toMutableList()
        val now = Instant.now()
        val toIgnore = mutableListOf<NewEvent>()
        val toDelete =
            prevEvents
                .values
                .filter { prev ->
                    !prev.state && Duration.between(prev.time, now) < props.turnOffDurationThreshold
                }.filter { prev ->
                    val excl = newEvents.filter { it.type == prev.type && it.state }
                    toIgnore.addAll(excl)
                    excl.isNotEmpty()
                }
        if (toIgnore.isNotEmpty()) {
            if (toIgnore.filterIsInstance<NewEvent.Initial>().isNotEmpty()) {
                log.warn {
                    "This should not happen. Detected initial new events in the list to ignore: $toIgnore"
                }
            }
            log.info {
                "Detected new events that occurred faster than ${props.turnOffDurationThreshold}. " +
                        "Ignoring new events: ${toIgnore.toList()}. Deleting old events: $toDelete"
            }
            newEvents.removeAll(toIgnore)
            toDelete
                .forEach {
                    userInteraction.deleteForEvent(it)
                    events.deleteEvent(it.id)
                }
        }
        return newEvents
    }

    private fun logInitialState(
        stateLogged: MutableMap<String, Boolean>,
        config: Config,
        prevState: PowerIspState,
        currentState: PowerIspState
    ) {
        if (stateLogged[config.address] != true) {
            log.info {
                "host=${config.address} stored state: $prevState, " +
                        "currently pinged state: $currentState"
            }
            stateLogged[config.address] = true
        }
    }
}
