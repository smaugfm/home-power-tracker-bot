package com.github.smaugfm.power.tracker.monitoring.network

import assertk.assertThat
import assertk.assertions.isTrue
import com.github.smaugfm.power.tracker.NoLiquibaseTestBase
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.net.InetAddress
import java.net.InetSocketAddress
import java.time.Duration

class PingImplTest : NoLiquibaseTestBase() {
    @Autowired
    private lateinit var service: Ping

    @Disabled
    @Test
    fun networkTest() {
        assertThat(
            service.isIcmpReachable(
                InetAddress.getByName("google.com"),
                Duration.ofSeconds(1),
                3
            )
        ).isTrue()
        assertThat(
            service.isTcpReachable(
                InetSocketAddress(InetAddress.getByName("google.com"), 443),
                Duration.ofSeconds(1),
                3
            )
        ).isTrue()
    }
}
