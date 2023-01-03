package com.github.smaugfm.power.tracker.integration

import com.github.smaugfm.power.tracker.monitoring.network.Ping
import java.net.InetAddress
import java.net.InetSocketAddress
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

class IntegrationPing : Ping {
    val icmp = AtomicBoolean(false)
    val tcp = AtomicBoolean(false)

    override fun isIcmpReachable(address: InetAddress, timeout: Duration) = icmp.get()

    override fun isTcpReachable(address: InetSocketAddress, timeout: Duration) = tcp.get()
}
