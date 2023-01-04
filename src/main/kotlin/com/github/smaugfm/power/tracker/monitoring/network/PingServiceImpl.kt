package com.github.smaugfm.power.tracker.monitoring.network

import com.github.smaugfm.power.tracker.dto.Monitorable
import com.github.smaugfm.power.tracker.dto.PowerIspState
import com.github.smaugfm.power.tracker.spring.MainLoopProperties
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.net.InetAddress
import java.net.InetSocketAddress

private val log = KotlinLogging.logger { }

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
                scope.async(Dispatchers.IO) {
                    ping.isTcpReachable(
                        InetSocketAddress(config.address, config.port.toInt()),
                        props.reachableTimeout
                    )
                },
                scope.async(Dispatchers.IO) {
                    ping.isIcmpReachable(
                        InetAddress.getByName(config.address),
                        props.reachableTimeout
                    )
                }).awaitAll()
            PowerIspState(power, isp)
        } else {
            PowerIspState(
                scope.async(Dispatchers.IO) {
                    ping.isIcmpReachable(
                        InetAddress.getByName(config.address),
                        props.reachableTimeout
                    )
                }.await(),
                null
            )
        }
}

