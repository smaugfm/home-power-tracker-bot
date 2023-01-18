package com.github.smaugfm.power.tracker.scripts

import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import java.time.Duration

@Disabled
class DeleteLessThanThresholdScript : ScriptBase() {

    @Value("\${app.loop.turn-off-duration-threshold}")
    private lateinit var threshold: Duration

    @Test
    fun deleteEventsLessThanThreshold() {
        val configId = 2L

        runBlocking {
            configsRepository.findAll().asFlow()
                .filter { it.id == configId }
                .collect { config ->
                    val events = eventsService.findAllEvents(config.id)
                        .toList()
                    events
                        .zip(events.drop(1))
                        .filter { (first, second) ->
                            Duration.between(first.time, second.time) < threshold
                        }.flatMap {
                            it.toList()
                        }
                        .forEach {
                            try {
                                userInteraction.deleteForEvent(it)
                            } catch (e: Throwable) {
                                println(e)
                            }
                            try {
                                this@DeleteLessThanThresholdScript.eventsService.deleteEvent(it.id)
                            } catch (e: Throwable) {
                                println(e)
                            }
                        }
                }
        }
    }

}
