import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import mu.KotlinLogging
import java.net.ConnectException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import kotlin.time.Duration
import kotlin.time.DurationUnit

private val log = KotlinLogging.logger { }

fun CoroutineScope.isIcmpReachable(address: InetAddress, timeout: Duration): Deferred<Boolean> {
    return async(Dispatchers.IO) {
        address.isReachable(timeout.toInt(DurationUnit.MILLISECONDS))
    }
}

fun CoroutineScope.isTcpReachable(
    address: InetSocketAddress,
    timeout: Duration
): Deferred<Boolean> {
    val socket = Socket()
    val timeoutMs = timeout.toInt(DurationUnit.MILLISECONDS)
    return async(Dispatchers.IO) {
        socket.soTimeout = timeoutMs
        try {
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
