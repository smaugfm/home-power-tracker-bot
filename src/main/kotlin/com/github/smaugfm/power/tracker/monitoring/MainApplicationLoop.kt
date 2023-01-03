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
        while (true) {
            try {
                if (!networkStability.waitStable())
                    continue

                configs.getAllMonitorable().collect { monitorable ->

                    val prevState = events.getCurrentState(monitorable.id)
                    val currentState = ping.ping(scope, monitorable)

                    if (prevState != currentState) {
                        events.calculateAddEvents(prevState, currentState, monitorable.id)
                            .collect { userInteraction.postEvent(it) }
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
    ): LaunchCoroutineBean = object : LaunchCoroutineBean {
        override suspend fun launch(scope: CoroutineScope) {
            userInteraction.deletionFlow().collect { eventId ->
                events.deleteAndGetLaterEvents(eventId).collect { updated ->
                    userInteraction.updateEvent(updated)
                }
            }
        }
    }

    @Bean
    fun exportLoopJob(
        userInteraction: UserInteractionService,
        events: EventsService
    ): LaunchCoroutineBean = object : LaunchCoroutineBean {
        override suspend fun launch(scope: CoroutineScope) {
            userInteraction.exportFlow().collect { configId ->
                userInteraction.exportEvents(configId, events.findAllEvents(configId))
            }
        }
    }
}
