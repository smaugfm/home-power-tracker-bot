package com.github.smaugfm.power.tracker.monitoring.network

import com.github.smaugfm.power.tracker.dto.Monitorable
import com.github.smaugfm.power.tracker.dto.PowerIspState
import com.github.smaugfm.power.tracker.spring.MainLoopProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import org.springframework.stereotype.Service
import java.net.InetAddress
import java.net.InetSocketAddress

@Service
class PingServiceImpl(
    protected val ping: Ping,
    private val props: MainLoopProperties,
) : PingService {
    override suspend fun ping(config: Monitorable) =
        supervisorScope {
            if (config.port != null) {
                PowerIspState(
                    async(Dispatchers.IO) {
                        ping.isTcpReachable(
                            InetSocketAddress(config.address, config.port.toInt()),
                            props.reachableTimeout
                        )
                    }.await(),
                    async(Dispatchers.IO) {
                        ping.isIcmpReachable(
                            InetAddress.getByName(config.address),
                            props.reachableTimeout
                        )
                    }.await()
                )
            } else {
                PowerIspState(
                    async(Dispatchers.IO) {
                        ping.isIcmpReachable(
                            InetAddress.getByName(config.address),
                            props.reachableTimeout
                        )
                    }.await(),
                    null
                )
            }
        }
}

