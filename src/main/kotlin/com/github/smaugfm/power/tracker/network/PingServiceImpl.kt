package com.github.smaugfm.power.tracker.network

import com.github.smaugfm.power.tracker.Config
import com.github.smaugfm.power.tracker.PowerIspState
import com.github.smaugfm.power.tracker.spring.MainLoopProperties
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitAll
import org.springframework.stereotype.Service

@Service
class PingServiceImpl(
    protected val ping: Ping,
    private val props: MainLoopProperties,
) : PingService {
    override suspend fun ping(scope: CoroutineScope, config: Config) =
        getState(scope, config)

    private suspend fun getState(scope: CoroutineScope, config: Config) =
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

