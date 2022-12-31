package com.github.smaugfm.power.tracker.monitoring.network

import com.github.smaugfm.power.tracker.TestBase
import com.github.smaugfm.power.tracker.interaction.UserInteractionService
import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import io.mockk.every
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.assertTimeout
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration
import org.springframework.test.context.TestPropertySource
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

@TestPropertySource(
    properties = [
        "app.network-stability.interval=100ms",
        "app.network-stability.timeout=200ms",
        "app.network-stability.wait-for-stable-network-timeout=600ms",
        "app.network-stability.consecutive-tries-to-consider-online=3",
        "app.network-stability.hosts=1.1.1.1",
        "logging.level.com.github.smaugfm.power.tracker.monitoring.network.NetworkStabilityServiceImpl=DEBUG"
    ]
)
@DelicateCoroutinesApi
@EnableAutoConfiguration(exclude = [LiquibaseAutoConfiguration::class])
class NetworkStabilityServiceImplTest : TestBase() {

    @MockkBean
    private lateinit var ping: Ping

    @MockkBean
    private lateinit var interaction: UserInteractionService

    @Autowired
    private lateinit var service: NetworkStabilityServiceImpl

    @Test
    fun simpleTest() {
        val reachable = AtomicBoolean(true)
        every {
            ping.isIcmpReachable(any(), any())
        } answers { reachable.get() }
        coEvery {
            interaction.postUnstableNetworkTimeout(any())
        } answers {}

        runBlocking {
            val job = GlobalScope.launch {
                service.launch(this)
            }
            assertTimeout(Duration.ofMillis(400)) {
                runBlocking {
                    service.waitStable()
                }
            }
            reachable.set(false)
            delay(200)
            assertThrows<TimeoutCancellationException> {
                runBlocking {
                    withTimeout(100) {
                        service.waitStable()
                    }
                }
            }
            println("reachable AGAIN")
            reachable.set(true)
            assertThrows<TimeoutCancellationException> {
                runBlocking {
                    withTimeout(100) {
                        service.waitStable()
                    }
                }
            }
            println("wait normal")
            delay(700)
            assertTimeout(Duration.ofMillis(100)) {
                runBlocking {
                    service.waitStable()
                }
            }
            job.cancel()
        }
    }
}
