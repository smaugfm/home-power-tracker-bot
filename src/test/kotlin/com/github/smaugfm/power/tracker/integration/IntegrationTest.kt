package com.github.smaugfm.power.tracker.integration

import com.github.smaugfm.power.tracker.Application
import com.github.smaugfm.power.tracker.RepositoryTestBase
import com.github.smaugfm.power.tracker.interaction.UserInteractionService
import com.github.smaugfm.power.tracker.monitoring.network.NetworkStabilityServiceImpl
import com.github.smaugfm.power.tracker.monitoring.network.Ping
import com.github.smaugfm.power.tracker.spring.NetworkStabilityProperties
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

@ActiveProfiles("integration")
@DelicateCoroutinesApi
@Disabled
@ContextConfiguration(classes = [IntegrationTest.IntegrationConfig::class])
class IntegrationTest : RepositoryTestBase() {
    @Autowired
    private lateinit var ping: IntegrationPing

    @Autowired
    private lateinit var stabilityBoolean: AtomicBoolean

    @Autowired
    private lateinit var userInteractionOperations: TestUserInteractionOperations

    @Autowired
    private lateinit var context: ConfigurableApplicationContext

    @Test
    fun networkStableUnstableTest() {
        saveConfig1()
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

    @Test
    fun deletionTest() {
        saveConfigNoPort()
        stabilityBoolean.set(true)
        ping.icmp.set(true)
        ping.tcp.set(true)
        GlobalScope.launch {
            println("LAUNCH icmp=true")
            Application.run(context)
        }
        Thread.sleep(1000)

        println("icmp=false")
        ping.icmp.set(false)
        Thread.sleep(1000)

        println("icmp=true")
        ping.icmp.set(true)
        Thread.sleep(1000)

        runBlocking {
            userInteractionOperations.deletionChannel.send(1)
        }
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
        @Primary
        fun networkStability(
            stabilityBoolean: AtomicBoolean,
            props: NetworkStabilityProperties,
            userInteractionService: UserInteractionService,
        ) =
            NetworkStabilityServiceImpl(object : Ping {
                override fun isIcmpReachable(
                    scope: CoroutineScope,
                    address: String,
                    eachTimeout: Duration,
                    tries: Int
                ) = CompletableDeferred(stabilityBoolean.get())

                override fun isTcpReachable(
                    scope: CoroutineScope,
                    address: String,
                    port: Int,
                    eachTimeout: Duration,
                    tries: Int
                ): Deferred<Boolean> {
                    fail("Should not be called")
                }

            }, props, userInteractionService)
    }
}


