package com.github.smaugfm.power.tracker.util

import mu.KotlinLogging
import java.net.ConnectException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.time.Duration

private val log = KotlinLogging.logger { }

object Ping {
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
