package com.github.smaugfm.power.tracker.stats

import com.github.smaugfm.power.tracker.NoLiquibaseTestBase
import com.github.smaugfm.power.tracker.YasnoGroup
import com.github.smaugfm.power.tracker.stats.image.YasnoScheduleImageGeneratorImpl
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.nio.file.Paths
import java.time.LocalDate
import kotlin.io.path.writeBytes

class YasnoScheduleImageGeneratorImplTest : NoLiquibaseTestBase() {

    @Autowired
    private lateinit var service: YasnoScheduleImageGeneratorImpl

    @Test
    fun createTestScreenShot() {
        runBlocking {
            val job = launch { service.launch(this) }
            val bytes = service.createSchedule(
                YasnoGroup.Group1,
                LocalDate.of(2023, 1, 16),
                listOf(
                    IntRange(0, 23),
                    IntRange(47, 48),
                    IntRange(144, 167),
                )
            )
            Paths.get("schedule-screenshot.png").writeBytes(bytes)
            job.cancel()
        }
    }
}
