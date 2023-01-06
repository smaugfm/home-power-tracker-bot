package com.github.smaugfm.power.tracker.monitoring.network

import com.github.smaugfm.power.tracker.dto.Monitorable
import com.github.smaugfm.power.tracker.dto.PowerIspState
import com.github.smaugfm.power.tracker.spring.MainLoopProperties
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitAll
import mu.KotlinLogging
import org.springframework.stereotype.Service

@Service
class PingServiceImpl(
    protected val ping: Ping,
    private val props: MainLoopProperties,
) : PingService {
    override suspend fun ping(scope: CoroutineScope, config: Monitorable) =
        getState(scope, config)

    private suspend fun getState(scope: CoroutineScope, config: Monitorable) =
        if (config.port != null) {
            val (power, isp) = listOf(
                ping.isTcpReachable(
                    scope,
                    config.address,
                    config.port,
                    props.reachableTimeout,
                    props.tries,
                ),
                ping.isIcmpReachable(
                    scope,
                    config.address,
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
                    config.address,
                    props.reachableTimeout,
                    props.tries
                ).await(),
                null
            )
        }
}

