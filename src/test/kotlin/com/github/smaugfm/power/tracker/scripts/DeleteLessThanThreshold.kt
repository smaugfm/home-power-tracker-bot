package com.github.smaugfm.power.tracker.scripts

import com.github.smaugfm.power.tracker.NoLiquibaseTestBase
import com.github.smaugfm.power.tracker.events.EventsService
import com.github.smaugfm.power.tracker.interaction.UserInteractionService
import com.github.smaugfm.power.tracker.persistence.ConfigsRepository
import com.github.smaugfm.power.tracker.stats.image.YasnoScheduleImageGeneratorImpl
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.test.context.TestPropertySource
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime

@TestPropertySource(
    properties = [
        "spring.r2dbc.url=r2dbc:postgresql://localhost:5432/postgres",
        "spring.r2dbc.username=postgres",
    ]
)
@Disabled
class DeleteLessThanThreshold : NoLiquibaseTestBase() {

    @Autowired
    private lateinit var service: YasnoScheduleImageGeneratorImpl

    @Autowired
    private lateinit var eventsService: EventsService

    @Autowired
    private lateinit var configsRepository: ConfigsRepository

    @Autowired
    private lateinit var userInteraction: UserInteractionService

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
                                this@DeleteLessThanThreshold.eventsService.deleteEvent(it.id)
                            } catch (e: Throwable) {
                                println(e)
                            }
                        }
                }
        }
    }

    @Test
    fun updateAllAfter() {
        val configId = 2L
        val time = ZonedDateTime.of(2022, 12, 20, 0, 0, 0, 0, ZoneId.systemDefault())
            .toInstant()


        runBlocking {
            val job = launch { service.launch(this) }

            eventsService
                .getEventsAfter(configId, time)
                .collect(userInteraction::updateForEvent)
            job.cancel()
        }
    }
}
