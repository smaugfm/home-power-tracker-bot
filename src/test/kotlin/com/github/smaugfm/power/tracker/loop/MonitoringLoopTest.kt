package com.github.smaugfm.power.tracker.loop

import com.github.smaugfm.power.tracker.Config
import com.github.smaugfm.power.tracker.PowerIspState
import com.github.smaugfm.power.tracker.RepositoryTestBase
import com.github.smaugfm.power.tracker.YasnoGroup
import com.github.smaugfm.power.tracker.interaction.UserInteractionService
import com.ninjasquad.springmockk.MockkBean
import io.mockk.coJustRun
import io.mockk.coVerify
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestPropertySource

@TestPropertySource(properties = ["app.loop.turn-off-duration-threshold=200ms"])
class MonitoringLoopTest : RepositoryTestBase() {

    @Autowired
    private lateinit var loop: MonitoringLoop

    @MockkBean
    private lateinit var userInteraction: UserInteractionService

    @Test
    fun test() {
        val config = saveConfig1()
        val id = config.id

        coJustRun { userInteraction.postForEvent(any()) }
        coJustRun { userInteraction.deleteForEvent(any()) }

        runBlocking {
            loop.processStateChange(
                Config(
                    id,
                    config.address,
                    YasnoGroup.Group1,
                    config.port
                ),
                PowerIspState(null, null),
                PowerIspState(true, true)
            )
            delay(100)
            loop.processStateChange(
                Config(
                    id,
                    config.address,
                    YasnoGroup.Group1,
                    config.port
                ),
                PowerIspState(true, true),
                PowerIspState(false, false)
            )
            delay(50)
            loop.processStateChange(
                Config(
                    id,
                    config.address,
                    YasnoGroup.Group1,
                    config.port
                ),
                PowerIspState(false, false),
                PowerIspState(true, true)
            )
            loop.processStateChange(
                Config(
                    id,
                    config.address,
                    YasnoGroup.Group1,
                    config.port
                ),
                PowerIspState(false, false),
                PowerIspState(true, true)
            )
            delay(250)
            loop.processStateChange(
                Config(
                    id,
                    config.address,
                    YasnoGroup.Group1,
                    config.port
                ),
                PowerIspState(false, false),
                PowerIspState(true, true)
            )
        }

        coVerify(exactly = 4) { userInteraction.postForEvent(any()) }
        coVerify(exactly = 4) { userInteraction.deleteForEvent(any()) }
    }
}
