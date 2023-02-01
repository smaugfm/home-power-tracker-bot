package com.github.smaugfm.power.tracker.network

import com.github.smaugfm.power.tracker.isZero
import kotlinx.coroutines.*
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.UnknownHostException
import java.time.Duration
import kotlin.time.toKotlinDuration

private val log = KotlinLogging.logger { }

@Component
class PingImpl : Ping {
    override fun isIcmpReachable(
        scope: CoroutineScope,
        address: String,
        eachTimeout: Duration,
        tries: Int
    ) =
        scope.async(Dispatchers.IO) {
            try {
                Runtime.getRuntime()
                    .exec(
                        "fping -c $tries -p " +
                                "${eachTimeout.toMillis()} ${InetAddress.getByName(address).hostAddress}"
                    )
                    .waitFor()
                    .isZero()
            } catch (e: UnknownHostException) {
                false
            } catch (e: Throwable) {
                log.error(e) { "Unexpected error executing fping" }
                false
            }
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
