package com.github.smaugfm.power.tracker

import com.github.smaugfm.power.tracker.config.ConfigService
import com.github.smaugfm.power.tracker.events.EventsService
import com.github.smaugfm.power.tracker.interaction.UserInteractionService
import com.github.smaugfm.power.tracker.network.PingService
import com.github.smaugfm.power.tracker.stats.StatsService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import java.time.Duration
import kotlin.time.toKotlinDuration

val log = KotlinLogging.logger { }

@SpringBootApplication
@EnableJpaRepositories
class Application {
    @Bean
    fun scope(): CoroutineScope = CoroutineScope(Dispatchers.Default)

    @Bean
    fun start(
        scope: CoroutineScope,
        configs: ConfigService,
        ping: PingService,
        events: EventsService,
        userInteraction: UserInteractionService,
        stats: StatsService,
        @Value("\${app.loop.interval}") interval: Duration,
    ): Job {
        return scope.launch(Dispatchers.IO) {
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
}

suspend fun main(args: Array<String>) {
    val context = runApplication<Application>(*args)
    context.getBeansOfType(Job::class.java).values.joinAll()
}
