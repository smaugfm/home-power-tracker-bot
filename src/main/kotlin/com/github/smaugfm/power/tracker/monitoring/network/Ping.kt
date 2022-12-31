package com.github.smaugfm.power.tracker.monitoring.network

import java.net.InetAddress
import java.net.InetSocketAddress
import java.time.Duration

interface Ping {
    fun isIcmpReachable(address: InetAddress, timeout: Duration): Boolean
    fun isTcpReachable(
        address: InetSocketAddress,
        timeout: Duration
    ): Boolean
}
