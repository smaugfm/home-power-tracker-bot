package com.github.smaugfm.power.tracker.spring

import com.github.smaugfm.power.tracker.config.ConfigService
import com.github.smaugfm.power.tracker.events.EventsService
import com.github.smaugfm.power.tracker.interaction.UserInteractionService
import com.github.smaugfm.power.tracker.network.PingService
import com.github.smaugfm.power.tracker.stats.StatsService
import kotlinx.coroutines.delay
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.auditing.DateTimeProvider
import java.time.Duration
import java.time.ZonedDateTime
import java.util.Optional
import kotlin.time.toKotlinDuration

@Configuration
class Config {
    @Bean
    fun zonedDateTimeProvider(): DateTimeProvider =
        DateTimeProvider { Optional.of(ZonedDateTime.now()) }

    @Bean
    fun mainLoop(
        configs: ConfigService,
        ping: PingService,
        events: EventsService,
        userInteraction: UserInteractionService,
        stats: StatsService,
        @Value("\${app.loop.interval}") interval: Duration,
    ): KotlinLaunchAdapter = object : KotlinLaunchAdapter {
        override suspend fun launch() {
            while (true) {
                configs.getAllMonitorable().collect { monitorable ->

                    val prevState = events.getCurrentState(monitorable.id)
                    val currentState = ping.ping(monitorable)

                    if (prevState != currentState) {
                        events.calculateAddEvents(prevState, currentState).forEach {
                            userInteraction.postEvent(it)
                        }
                    }
                }

                delay(interval.toKotlinDuration())
            }
        }
    }

    @Bean
    fun deletionLoopJob(
        userInteraction: UserInteractionService,
        events: EventsService,
    ): KotlinLaunchAdapter = object : KotlinLaunchAdapter {
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
    ): KotlinLaunchAdapter = object : KotlinLaunchAdapter {
        override suspend fun launch() {
            userInteraction.exportFlow().collect { configId ->
                userInteraction.exportEvents(configId, events.getAllEvents(configId))
            }
        }
    }
}
