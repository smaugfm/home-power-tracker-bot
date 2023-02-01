package com.github.smaugfm.power.tracker.network

import com.github.smaugfm.power.tracker.PowerIspState
import com.github.smaugfm.power.tracker.spring.LoopProperties
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitAll
import mu.KotlinLogging
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger { }

@Service
class PingServiceImpl(
    protected val ping: Ping,
    private val props: LoopProperties,
) : PingService {
    override suspend fun ping(scope: CoroutineScope, address: String, port: Int?) =
        getState(scope, address, port)

    private suspend fun getState(scope: CoroutineScope, address: String, port: Int?) =
        if (port != null) {
            val (power, isp) = listOf(
                ping.isTcpReachable(
                    scope,
                    address,
                    port,
                    props.reachableTimeout,
                    props.tries,
                ),
                ping.isIcmpReachable(
                    scope,
                    address,
                    props.reachableTimeout,
                    props.tries
                )
            ).awaitAll()
            PowerIspState(
                power, isp
            )
        } else {
            PowerIspState(
                ping.isIcmpReachable(
                    scope,
                    address,
                    props.reachableTimeout,
                    props.tries
                ).await(),
                null
            )
        }
}

