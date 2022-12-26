package com.github.smaugfm.power.tracker.network

import mu.KotlinLogging
import java.net.ConnectException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import kotlin.time.Duration
import kotlin.time.DurationUnit

private val log = KotlinLogging.logger { }

fun isIcmpReachable(address: InetAddress, timeout: Duration): Boolean =
    address.isReachable(timeout.toInt(DurationUnit.MILLISECONDS))

fun isTcpReachable(
    address: InetSocketAddress,
    timeout: Duration
): Boolean {
    val socket = Socket()
    val timeoutMs = timeout.toInt(DurationUnit.MILLISECONDS)
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
