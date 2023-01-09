package com.github.smaugfm.power.tracker.network

import com.github.smaugfm.power.tracker.util.isZero
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import org.springframework.stereotype.Component
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.time.Duration
import kotlin.time.toKotlinDuration

@Component
class PingImpl : Ping {
    override fun isIcmpReachable(
        scope: CoroutineScope,
        address: String,
        eachTimeout: Duration,
        tries: Int
    ) =
        scope.async(Dispatchers.IO) {
            Runtime.getRuntime()
                .exec(
                    "fping -c $tries -p " +
                            "${eachTimeout.toMillis()} ${InetAddress.getByName(address).hostAddress}"
                )
                .waitFor()
                .isZero()
        }

    override fun isTcpReachable(
        scope: CoroutineScope,
        address: String,
        port: Int,
        eachTimeout: Duration,
        tries: Int
    ): Deferred<Boolean> {
        val timeoutMs = eachTimeout.toMillis().toInt()
        val interval = eachTimeout.dividedBy(2).toKotlinDuration()
        return scope.async(Dispatchers.IO) {
            val connect = {
                val socket = Socket()
                socket.soTimeout = timeoutMs

                try {
                    socket.connect(InetSocketAddress(address, port), timeoutMs)
                    true
                } catch (e: Throwable) {
                    false
                }
            }

            var attempt = 0
            do {
                if (connect())
                    return@async true
                delay(interval)
            } while (attempt++ < tries)
            return@async false
        }
    }
}
