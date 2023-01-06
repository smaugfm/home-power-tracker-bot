package com.github.smaugfm.power.tracker.monitoring.network

import com.github.smaugfm.power.tracker.interaction.UserInteractionService
import com.github.smaugfm.power.tracker.spring.LaunchCoroutineBean
import com.github.smaugfm.power.tracker.spring.NetworkStabilityProperties
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.time.withTimeout
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.springframework.stereotype.Component
import kotlin.time.toKotlinDuration

private val log = KotlinLogging.logger { }

@Component
@DelicateCoroutinesApi
class NetworkStabilityServiceImpl(
    protected val ping: Ping,
    private val props: NetworkStabilityProperties,
    private val userInteractionService: UserInteractionService,
) : LaunchCoroutineBean, NetworkStabilityService {
    private var status = false
    private var successfulTriesAfterDrop = 0
    private var context = newSingleThreadContext("network-monitor")

    @Volatile
    private var networkStableDeferred: CompletableDeferred<Unit>? = CompletableDeferred()

    override suspend fun waitStable(): Boolean {
        val def = networkStableDeferred
        return if (def != null)
            try {
                withTimeout(props.waitForStableNetworkTimeout) {
                    log.info { "Waiting for stable network..." }
                    networkStableDeferred?.await()
                }
                true
            } catch (e: TimeoutCancellationException) {
                log.error("Timed out waiting on stable network. Notifying users...")
                userInteractionService.postUnstableNetworkTimeout(props.waitForStableNetworkTimeout)
                false
            }
        else
            true
    }

    override suspend fun launch(scope: CoroutineScope) {
        withContext(context) {
            while (true) {
                try {
                    val results = isOnline(scope)
                    val online = results.all { it.second }
                    if (!online) {
                        log.info {
                            "Failed to reach hosts: ${
                                results.filter { !it.second }.joinToString(", ")
                            }"
                        }
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
                            log.warn {
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

    private suspend fun isOnline(scope: CoroutineScope): List<Pair<String, Boolean>> =
        props.hosts.zip(
            props.hosts.map { host ->
                ping.isIcmpReachable(
                    scope,
                    host,
                    props.timeout,
                    props.tries
                )
            }.awaitAll()
        )
}
