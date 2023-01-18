package com.github.smaugfm.power.tracker.scripts

import com.github.smaugfm.power.tracker.stats.image.YasnoScheduleImageGeneratorImpl
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.ZoneId
import java.time.ZonedDateTime

@Disabled
class UpdateEventsScript : ScriptBase() {
    @Autowired
    private lateinit var service: YasnoScheduleImageGeneratorImpl

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
