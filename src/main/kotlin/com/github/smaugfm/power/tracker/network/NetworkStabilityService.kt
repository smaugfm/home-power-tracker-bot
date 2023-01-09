package com.github.smaugfm.power.tracker.network

interface NetworkStabilityService {
    suspend fun waitStable(): Boolean
}
