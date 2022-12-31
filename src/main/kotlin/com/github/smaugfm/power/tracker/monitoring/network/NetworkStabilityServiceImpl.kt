package com.github.smaugfm.power.tracker.monitoring.network

import com.github.smaugfm.power.tracker.interaction.UserInteractionService
import com.github.smaugfm.power.tracker.spring.LaunchCoroutineBean
import com.github.smaugfm.power.tracker.spring.NetworkStabilityProperties
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.time.withTimeout
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.net.InetAddress
import kotlin.system.exitProcess
import kotlin.time.toKotlinDuration

private val log = KotlinLogging.logger { }

@Component
@DelicateCoroutinesApi
class NetworkStabilityServiceImpl(
    protected val ping: Ping,
    private val props: NetworkStabilityProperties,
    private val userInteractionService: UserInteractionService,
) : LaunchCoroutineBean, NetworkStabilityService {
    private var status = true
    private var successfulTriesAfterDrop = 0
    private var context = newSingleThreadContext("network-monitor")

    @Volatile
    private var networkStableDeferred: CompletableDeferred<Unit>? = null

    override suspend fun waitStable(): Boolean {
        val def = networkStableDeferred
        if (def != null)
            return try {
                withTimeout(props.waitForStableNetworkTimeout) {
                    networkStableDeferred?.await()
                }
                true
            } catch (e: TimeoutCancellationException) {
                log.error("Timed out waiting on stable network. Notifying users...")
                userInteractionService.postUnstableNetworkTimeout(props.waitForStableNetworkTimeout)
                false
            }
        return true
    }

    override suspend fun launch(scope: CoroutineScope) {
        withContext(context) {
            while (true) {
                try {
                    val results = isOnline()
                    val online = results.all { it.second }
                    if (!online) {
                        if (successfulTriesAfterDrop > 1) {
                            log.info(
                                "Network still has issues. " +
                                        "Check attempt #${successfulTriesAfterDrop} failed"
                            )
                        }
                        successfulTriesAfterDrop = 0
                    }

                    if (online != status) {
                        if (online) {
                            if (++successfulTriesAfterDrop >= props.consecutiveTriesToConsiderOnline) {
                                status = true
                                successfulTriesAfterDrop = 0
                                log.info { "Network is STABLE again" }
                                networkStableDeferred?.complete(Unit)
                                networkStableDeferred = null
                            }
                        } else {
                            status = false
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
                    status = false
                }

                delay(props.interval.toKotlinDuration())
            }
        }
    }

    private suspend fun isOnline() =
        withContext(Dispatchers.IO) {
            props.hosts.map {
                log.debug { "Trying to reach $it..." }
                it to ping.isIcmpReachable(
                    InetAddress.getByName(it),
                    props.timeout
                )
            }
        }
}
