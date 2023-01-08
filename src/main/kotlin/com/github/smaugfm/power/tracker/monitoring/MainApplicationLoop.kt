package com.github.smaugfm.power.tracker.monitoring

import com.github.smaugfm.power.tracker.config.ConfigService
import com.github.smaugfm.power.tracker.events.EventsService
import com.github.smaugfm.power.tracker.interaction.UserInteractionService
import com.github.smaugfm.power.tracker.monitoring.network.NetworkStabilityService
import com.github.smaugfm.power.tracker.monitoring.network.PingService
import com.github.smaugfm.power.tracker.spring.LaunchCoroutineBean
import com.github.smaugfm.power.tracker.spring.MainLoopProperties
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import mu.KotlinLogging
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import kotlin.time.toKotlinDuration

private val log = KotlinLogging.logger { }

@Component
class MainApplicationLoop(
    private val configs: ConfigService,
    private val ping: PingService,
    private val networkStability: NetworkStabilityService,
    private val events: EventsService,
    private val userInteraction: UserInteractionService,
    private val props: MainLoopProperties,
) : LaunchCoroutineBean {
    override suspend fun launch(scope: CoroutineScope) {
        val stateLogged = mutableMapOf<String, Boolean>()
        while (true) {
            try {
                if (!networkStability.waitStable())
                    continue

                configs.getAllMonitorable().collect { monitorable ->

                    val prevState = events.getCurrentState(monitorable.id)
                    val currentState = ping.ping(scope, monitorable)
                    if (stateLogged[monitorable.address] != true) {
                        log.info {
                            "host=${monitorable.address} stored state: $prevState, " +
                                    "currently pinged state: $currentState"
                        }
                        stateLogged[monitorable.address] = true
                    }

                    if (prevState != currentState) {
                        log.info {
                            "State diff for host=${monitorable.address}. " +
                                    "prev: $prevState, cur: $currentState"
                        }
                        events.calculateAddEvents(prevState, currentState, monitorable.id)
                            .collect { userInteraction.postForEvent(it) }
                    }
                }
            } catch (e: Throwable) {
                log.error(e) { "Error in main loop." }
            }

            delay(props.interval.toKotlinDuration())
        }
    }

    @Bean
    fun deletionLoopJob(
        userInteraction: UserInteractionService,
        events: EventsService,
    ) = object : LaunchCoroutineBean {
        override suspend fun launch(scope: CoroutineScope) {
            userInteraction.deletionFlow().collect { eventId ->
                val event = events.getEvent(eventId)
                if (event == null) {
                    log.warn { "Missing event for eventId=$eventId" }
                    return@collect
                }
                userInteraction.deleteForEvent(event)
                events.deleteEvent(eventId)

                events
                    .getEventsAfter(event.configId, event.time)
                    .collect(userInteraction::updateForEvent)
            }
        }
    }

    @Bean
    fun exportLoopJob(
        userInteraction: UserInteractionService,
        events: EventsService
    ) = object : LaunchCoroutineBean {
        override suspend fun launch(scope: CoroutineScope) {
            userInteraction.exportFlow().collect { configId ->
                userInteraction.exportEvents(configId, events.findAllEvents(configId))
            }
        }
    }
}
