package com.github.smaugfm.power.tracker.monitoring.network

import com.github.smaugfm.power.tracker.util.isZero
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.time.Duration

private val log = KotlinLogging.logger { }

@Component
class PingImpl : Ping {
    override fun isIcmpReachable(address: InetAddress, timeout: Duration): Boolean =
        Runtime.getRuntime()
            .exec("ping -c 1 -W ${timeout.toSeconds()} ${address.hostAddress}")
            .waitFor()
            .isZero()

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
        } catch (e: Throwable) {
            false
        }
    }
}
