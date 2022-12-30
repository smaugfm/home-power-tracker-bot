package com.github.smaugfm.power.tracker.monitoring.network

import com.github.smaugfm.power.tracker.interaction.UserInteractionService
import com.github.smaugfm.power.tracker.spring.LaunchCoroutineBean
import com.github.smaugfm.power.tracker.spring.NetworkStabilityProperties
import com.github.smaugfm.power.tracker.util.Ping
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.time.withTimeout
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.net.InetAddress
import kotlin.system.exitProcess
import kotlin.time.toKotlinDuration

private val log = KotlinLogging.logger { }

@Component
@DelicateCoroutinesApi
class NetworkStabilityServiceImpl(
    private val props: NetworkStabilityProperties,
    private val userInteractionService: UserInteractionService,
) : LaunchCoroutineBean, NetworkStabilityService {
    private var isStable = false
    private var successfulTriesAfterDrop = 0
    private val context = newSingleThreadContext("network-monitor")

    @Volatile
    private var networkStableDeferred: CompletableDeferred<Unit>? = null

    override suspend fun waitStable() {
        val def = networkStableDeferred
        if (def != null)
            try {
                withTimeout(props.stableNetworkTimeout) {
                    networkStableDeferred?.await()
                }
            } catch (e: TimeoutCancellationException) {
                log.error("Timed out waiting on stable network. Notifying users...")
                userInteractionService.postUnstableNetworkTimeout(props.stableNetworkTimeout)
                log.error("Exiting application...")
                exitProcess(1)
            }
    }

    override suspend fun launch(scope: CoroutineScope) {
        withContext(context) {
            while (true) {
                try {
                    val results = withContext(Dispatchers.IO) {
                        props.hosts.map {
                            it to Ping.isIcmpReachable(
                                InetAddress.getByName(it),
                                props.timeout
                            )
                        }
                    }
                    val latestIsStable = results.all { it.second }
                    if (!isStable) {
                        if (successfulTriesAfterDrop > 1) {
                            log.info(
                                "Network still has issues. " +
                                        "Check attempt #${successfulTriesAfterDrop} failed"
                            )
                        }
                        successfulTriesAfterDrop = 0
                    }

                    if (latestIsStable != isStable) {
                        if (latestIsStable) {
                            if (++successfulTriesAfterDrop >= props.consecutiveTriesToConsiderOnline) {
                                isStable = true
                                successfulTriesAfterDrop = 0
                                log.info { "Network is STABLE again" }
                                networkStableDeferred?.complete(Unit)
                                networkStableDeferred = null
                            }
                        } else {
                            isStable = false
                            log.error {
                                "Network ISSUES detected. Failed to reach: ${
                                    results.filter { !it.second }
                                        .joinToString(", ") { it.first }
                                }"
                            }
                            networkStableDeferred = CompletableDeferred()
                        }
                    }
                } catch (e: Throwable) {
                    log.error(e) { "An error occurred while monitoring network connectivity" }
                    isStable = false
                }

                delay(props.interval.toKotlinDuration())
            }
        }
    }
}
