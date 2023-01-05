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
    override fun isIcmpReachable(address: InetAddress, eachTimeout: Duration, tries: Int): Boolean =
        Runtime.getRuntime()
            .exec("fping -c $tries -p ${eachTimeout.toMillis()} ${address.hostAddress}")
            .waitFor()
            .isZero()

    override fun isTcpReachable(
        address: InetSocketAddress,
        eachTimeout: Duration,
        tries: Int
    ): Boolean {
        val socket = Socket()
        val timeoutMs = eachTimeout.toMillis().toInt()
        socket.soTimeout = timeoutMs
        val connect = {
            try {
                socket.connect(address, timeoutMs)
                true
            } catch (e: Throwable) {
                false
            }
        }
        var attempt = 0
        do {
            if (connect())
                return true
        } while (attempt++ < tries)
        return false
    }
}
