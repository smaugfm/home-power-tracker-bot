package com.github.smaugfm.power.tracker.monitoring.network

import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.net.ConnectException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.time.Duration

private val log = KotlinLogging.logger { }

@Component
class PingImpl : Ping {
    override fun isIcmpReachable(address: InetAddress, timeout: Duration): Boolean {
        val result = Runtime.getRuntime()
            .exec("ping -c 1 -t ${timeout.toSeconds()} ${address.hostAddress}")
            .waitFor()
        return (result == 0).also {
            if (!it)
                log.debug { "Address $address is not reachable by ICMP" }
        }
    }

    override fun isTcpReachable(
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
            log.warn { "TCP ping to $address: timed out." }
            false
        } catch (e: ConnectException) {
            log.warn { "TCP ping to $address: connection refused" }
            false
        } catch (e: Throwable) {
            log.warn { "TCP ping to $address: unknown error" }
            false
        }
    }
}
