package com.github.smaugfm.power.tracker.monitoring.network

interface NetworkStabilityService {
    suspend fun waitStable()
}
