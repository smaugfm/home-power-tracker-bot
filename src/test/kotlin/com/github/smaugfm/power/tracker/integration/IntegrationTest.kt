package com.github.smaugfm.power.tracker.integration

import com.github.smaugfm.power.tracker.Application
import com.github.smaugfm.power.tracker.RepositoryTestBase
import com.github.smaugfm.power.tracker.interaction.UserInteractionService
import com.github.smaugfm.power.tracker.monitoring.network.NetworkStabilityServiceImpl
import com.github.smaugfm.power.tracker.monitoring.network.Ping
import com.github.smaugfm.power.tracker.spring.NetworkStabilityProperties
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import java.net.InetAddress
import java.net.InetSocketAddress
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

@TestPropertySource(
    properties = [
        "app.loop.interval=300ms",
        "app.loop.reachable-timeout=300ms",
        "app.network-stability.interval=300ms",
        "app.network-stability.timeout=500ms",
        "app.network-stability.wait-for-stable-network-timeout=1s",
        "app.network-stability.consecutive-tries-to-consider-online=3",
        "app.network-stability.hosts=1.1.1.1",
        "logging.level.com.github.smaugfm.power.tracker.monitoring.network.NetworkStabilityServiceImpl=DEBUG"
    ]
)
@DelicateCoroutinesApi
@ContextConfiguration(classes = [IntegrationTest.IntegrationConfig::class])
class IntegrationTest : RepositoryTestBase() {
    @Autowired
    private lateinit var ping: IntegrationPing

    @Autowired
    private lateinit var stabilityBoolean: AtomicBoolean

    @Autowired
    private lateinit var context: ConfigurableApplicationContext

    @Test
    fun integration() {
        val configId = saveConfig1()
        stabilityBoolean.set(false)

        GlobalScope.launch {
            println("LAUNCH")
            Application.run(context)
        }
        Thread.sleep(1000)

        println("NETWORK STABLE")
        stabilityBoolean.set(true)
        Thread.sleep(1000)

        println("PINGS TRUE")
        ping.icmp.set(true)
        ping.tcp.set(true)
        Thread.sleep(1000)

        println("NETWORK UNSTABLE")
        stabilityBoolean.set(false)
        Thread.sleep(1000)

        println("POWER DOWN")
        ping.tcp.set(false)
        Thread.sleep(1000)

        println("NETWORK STABLE")
        stabilityBoolean.set(true)
        Thread.sleep(1000)
    }

    @Configuration
    class IntegrationConfig {
        @Bean
        fun userInteraction() = TestUserInteractionOperations()

        @Bean
        @Primary
        fun testPingImpl() = IntegrationPing()

        @Bean
        fun stabilityBoolean() = AtomicBoolean(false)

        @Bean
        fun networkStability(
            stabilityBoolean: AtomicBoolean,
            props: NetworkStabilityProperties,
            userInteractionService: UserInteractionService,
        ) =
            NetworkStabilityServiceImpl(object : Ping {
                override fun isIcmpReachable(address: InetAddress, timeout: Duration) =
                    stabilityBoolean.get()

                override fun isTcpReachable(
                    address: InetSocketAddress,
                    timeout: Duration
                ): Boolean {
                    throw IllegalStateException("Should not be called")
                }
            }, props, userInteractionService)
    }
}


