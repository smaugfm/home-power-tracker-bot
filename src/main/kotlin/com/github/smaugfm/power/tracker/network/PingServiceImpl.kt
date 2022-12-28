package com.github.smaugfm.power.tracker.network

import com.github.smaugfm.power.tracker.dto.Monitorable
import com.github.smaugfm.power.tracker.dto.PowerIspState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.ConnectException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.time.Duration

private val log = KotlinLogging.logger { }

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
                        isTcpReachable(
                            InetSocketAddress(config.address, config.port.toInt()),
                            timeout
                        )
                    }.await(),
                    async(Dispatchers.IO) {
                        isIcmpReachable(InetAddress.getByName(config.address), timeout)
                    }.await()
                )
            } else {
                PowerIspState(
                    async(Dispatchers.IO) {
                        isIcmpReachable(
                            InetAddress.getByName(config.address),
                            timeout
                        )
                    }.await(),
                    null
                )
            }
        }

    fun isIcmpReachable(address: InetAddress, timeout: Duration): Boolean =
        address.isReachable(timeout.toMillis().toInt())

    fun isTcpReachable(
        address: InetSocketAddress,
        timeout: Duration
    ): Boolean {
        val socket = Socket()
        val timeoutMs = timeout.toMillis().toInt()
        socket.soTimeout = timeoutMs
        return try {
            socket.connect(address, timeoutMs)
            true
        } catch (e: SocketTimeoutException) {
            log.info { "TCP ping to $address: timed out." }
            false
        } catch (e: ConnectException) {
            log.info { "TCP ping to $address: connection refused" }
            false
        }
    }
}

