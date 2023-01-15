package com.github.smaugfm.power.tracker.stats

import com.github.smaugfm.power.tracker.Event
import com.github.smaugfm.power.tracker.EventType
import com.github.smaugfm.power.tracker.NoLiquibaseTestBase
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant

class ScreenshotSummaryStatsServiceTest : NoLiquibaseTestBase() {

    @Autowired
    private lateinit var service: ScreenshotSummaryStatsService

    @Test
    fun test() {
        runBlocking {
            service.calculate(Event(0, false, EventType.POWER, 0, Instant.now()))
        }
    }
}
