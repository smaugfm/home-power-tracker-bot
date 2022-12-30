package com.github.smaugfm.power.tracker.monitoring.network

import kotlinx.coroutines.channels.ReceiveChannel

interface NetworkStabilityService {
    suspend fun waitStable()
}
