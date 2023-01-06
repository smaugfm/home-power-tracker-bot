package com.github.smaugfm.power.tracker.monitoring.network

import assertk.assertThat
import assertk.assertions.isTrue
import com.github.smaugfm.power.tracker.NoLiquibaseTestBase
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration

class PingImplTest : NoLiquibaseTestBase() {
    @Autowired
    private lateinit var service: Ping

    @Disabled
    @Test
    fun networkTest() {
        runBlocking {
            assertThat(
                service.isIcmpReachable(
                    this,
                    "google.com",
                    Duration.ofSeconds(1),
                    3
                ).await()
            ).isTrue()
            assertThat(
                service.isTcpReachable(
                    this,
                    "google.com",
                    443,
                    Duration.ofSeconds(1),
                    3
                ).await()
            ).isTrue()
        }
    }
}
