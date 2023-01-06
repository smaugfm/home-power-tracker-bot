package com.github.smaugfm.power.tracker.integration

import com.github.smaugfm.power.tracker.monitoring.network.Ping
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

class IntegrationPing : Ping {
    val icmp = AtomicBoolean(false)
    val tcp = AtomicBoolean(false)
    override fun isIcmpReachable(
        scope: CoroutineScope,
        address: String,
        eachTimeout: Duration,
        tries: Int
    ): Deferred<Boolean> = CompletableDeferred(icmp.get())

    override fun isTcpReachable(
        scope: CoroutineScope,
        address: String,
        port: Int,
        eachTimeout: Duration,
        tries: Int
    ) = CompletableDeferred(tcp.get())
}
