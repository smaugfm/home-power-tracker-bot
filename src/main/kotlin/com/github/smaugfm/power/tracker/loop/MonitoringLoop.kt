package com.github.smaugfm.power.tracker.loop

import com.github.smaugfm.power.tracker.config.ConfigService
import com.github.smaugfm.power.tracker.events.EventsService
import com.github.smaugfm.power.tracker.interaction.UserInteractionService
import com.github.smaugfm.power.tracker.network.NetworkStabilityService
import com.github.smaugfm.power.tracker.network.PingService
import com.github.smaugfm.power.tracker.spring.LaunchCoroutineBean
import com.github.smaugfm.power.tracker.spring.MainLoopProperties
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import mu.KotlinLogging
import org.springframework.stereotype.Component
import kotlin.time.toKotlinDuration

private val log = KotlinLogging.logger { }

@Component
class MonitoringLoop(
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

                configs.getAll().collect { config ->

                    val prevState = events.getCurrentState(config.id)
                    val currentState = ping.ping(scope, config)
                    if (stateLogged[config.address] != true) {
                        log.info {
                            "host=${config.address} stored state: $prevState, " +
                                    "currently pinged state: $currentState"
                        }
                        stateLogged[config.address] = true
                    }

                    if (prevState != currentState) {
                        log.info {
                            "State diff for host=${config.address}. " +
                                    "prev: $prevState, cur: $currentState"
                        }
                        events.calculateAddEvents(prevState, currentState, config.id)
                            .collect { userInteraction.postForEvent(it) }
                    }
                }
            } catch (e: Throwable) {
                log.error(e) { "Error in main loop." }
            }

            delay(props.interval.toKotlinDuration())
        }
    }
}
