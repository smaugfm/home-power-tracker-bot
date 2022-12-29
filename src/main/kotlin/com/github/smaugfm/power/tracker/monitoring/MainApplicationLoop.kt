package com.github.smaugfm.power.tracker.monitoring

import com.github.smaugfm.power.tracker.config.ConfigService
import com.github.smaugfm.power.tracker.events.EventsService
import com.github.smaugfm.power.tracker.interaction.UserInteractionService
import com.github.smaugfm.power.tracker.monitoring.network.NetworkStabilityService
import com.github.smaugfm.power.tracker.monitoring.network.PingService
import com.github.smaugfm.power.tracker.spring.CoroutinesLaunchAdapter
import kotlinx.coroutines.delay
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import java.time.Duration
import kotlin.time.toKotlinDuration

@Component
class MainApplicationLoop(
    private val configs: ConfigService,
    private val ping: PingService,
    private val networkStability: NetworkStabilityService,
    private val events: EventsService,
    private val userInteraction: UserInteractionService,
    @Value("\${app.loop.interval}")
    private val interval: Duration,
) : CoroutinesLaunchAdapter {
    override suspend fun launch() {
        while (true) {
            networkStability.waitStable()
            configs.getAllMonitorable().collect { monitorable ->

                val prevState = events.getCurrentState(monitorable.id)
                val currentState = ping.ping(monitorable)

                if (prevState != currentState) {
                    events.calculateAddEvents(prevState, currentState, monitorable.id)
                        .collect { userInteraction.postEvent(it) }
                }
            }

            delay(interval.toKotlinDuration())
        }
    }

    @Bean
    fun deletionLoopJob(
        userInteraction: UserInteractionService,
        events: EventsService,
    ): CoroutinesLaunchAdapter = object : CoroutinesLaunchAdapter {
        override suspend fun launch() {
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
    ): CoroutinesLaunchAdapter = object : CoroutinesLaunchAdapter {
        override suspend fun launch() {
            userInteraction.exportFlow().collect { configId ->
                userInteraction.exportEvents(configId, events.getAllEvents(configId))
            }
        }
    }
}
