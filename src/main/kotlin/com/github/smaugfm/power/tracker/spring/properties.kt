package com.github.smaugfm.power.tracker.spring

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "app.loop")
data class MainLoopProperties(
    val interval: Duration,
    val reachableTimeout: Duration,
    val tries: Int
)

@ConfigurationProperties(prefix = "app.network-stability")
data class NetworkStabilityProperties(
    val interval: Duration,
    val timeout: Duration,
    val tries: Int,
    val waitForStableNetworkTimeout: Duration,
    val consecutiveTriesToConsiderOnline: Int,
    val hosts: List<String>,
)
