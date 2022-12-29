package com.github.smaugfm.power.tracker.monitoring.network

import com.github.smaugfm.power.tracker.dto.Monitorable
import com.github.smaugfm.power.tracker.dto.PowerIspState
import com.github.smaugfm.power.tracker.util.Ping
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.InetAddress
import java.net.InetSocketAddress
import java.time.Duration

@Service
class PingServiceImpl(
    @Value("\${app.loop.reachable-timeout}")
    private val timeout: Duration
) : PingService {
    override suspend fun ping(config: Monitorable) =
        supervisorScope {
            if (config.port != null) {
                PowerIspState(
                    async(Dispatchers.IO) {
                        Ping.isTcpReachable(
                            InetSocketAddress(config.address, config.port.toInt()),
                            timeout
                        )
                    }.await(),
                    async(Dispatchers.IO) {
                        Ping.isIcmpReachable(InetAddress.getByName(config.address), timeout)
                    }.await()
                )
            } else {
                PowerIspState(
                    async(Dispatchers.IO) {
                        Ping.isIcmpReachable(
                            InetAddress.getByName(config.address),
                            timeout
                        )
                    }.await(),
                    null
                )
            }
        }
}

