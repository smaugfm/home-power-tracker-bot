package com.github.smaugfm.power.tracker.monitoring.network

import com.github.smaugfm.power.tracker.dto.Monitorable
import com.github.smaugfm.power.tracker.dto.PowerIspState
import com.github.smaugfm.power.tracker.spring.LaunchCoroutineBean
import com.github.smaugfm.power.tracker.spring.MainLoopProperties
import com.github.smaugfm.power.tracker.util.Ping
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.consume
import kotlinx.coroutines.supervisorScope
import org.springframework.stereotype.Service
import java.net.InetAddress
import java.net.InetSocketAddress

@Service
class PingServiceImpl(
    private val props: MainLoopProperties,
) : PingService {
    override suspend fun ping(config: Monitorable) =
        supervisorScope {
            if (config.port != null) {
                PowerIspState(
                    async(Dispatchers.IO) {
                        Ping.isTcpReachable(
                            InetSocketAddress(config.address, config.port.toInt()),
                            props.reachableTimeout
                        )
                    }.await(),
                    async(Dispatchers.IO) {
                        Ping.isIcmpReachable(
                            InetAddress.getByName(config.address),
                            props.reachableTimeout
                        )
                    }.await()
                )
            } else {
                PowerIspState(
                    async(Dispatchers.IO) {
                        Ping.isIcmpReachable(
                            InetAddress.getByName(config.address),
                            props.reachableTimeout
                        )
                    }.await(),
                    null
                )
            }
        }
}

