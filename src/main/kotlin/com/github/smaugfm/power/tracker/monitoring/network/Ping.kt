package com.github.smaugfm.power.tracker.monitoring.network

import java.net.InetAddress
import java.net.InetSocketAddress
import java.time.Duration

interface Ping {
    fun isIcmpReachable(
        address: InetAddress,
        eachTimeout: Duration,
        tries: Int
    ): Boolean

    fun isTcpReachable(
        address: InetSocketAddress,
        eachTimeout: Duration,
        tries: Int
    ): Boolean
}
