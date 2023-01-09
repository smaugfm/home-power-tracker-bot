package com.github.smaugfm.power.tracker.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import java.time.Duration

interface Ping {
    fun isIcmpReachable(
        scope: CoroutineScope,
        address: String,
        eachTimeout: Duration,
        tries: Int
    ): Deferred<Boolean>

    fun isTcpReachable(
        scope: CoroutineScope,
        address: String,
        port: Int,
        eachTimeout: Duration,
        tries: Int
    ): Deferred<Boolean>
}
