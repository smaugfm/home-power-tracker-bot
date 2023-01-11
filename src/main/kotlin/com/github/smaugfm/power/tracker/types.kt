package com.github.smaugfm.power.tracker

import java.time.Duration
import java.time.Instant

data class Event(
    val id: EventId,
    val state: Boolean,
    val type: EventType,
    val configId: ConfigId,
    val time: Instant
) {
    fun since(earlier: Event): Duration = Duration.between(earlier.time, time)
}

typealias EventId = Long
typealias ConfigId = Long

enum class EventType {
    POWER,
    ISP
}

data class PowerIspState(
    val hasPower: Boolean?,
    val hasIsp: Boolean?
)

data class Config(
    val id: ConfigId,
    val address: String,
    val port: Int?,
)

sealed class SummaryStatsPeriod {
    object Week : SummaryStatsPeriod()
    object Month : SummaryStatsPeriod()
    object Year : SummaryStatsPeriod()
    data class Custom(val days: Int) : SummaryStatsPeriod()
}

data class PeriodicStats(
    val state: Boolean,
    val longestPeriod: Duration,
    val shortestPeriod: Duration,
    val medianPeriod: Duration
)

interface LastInverseStats {
    val lastInverse: Duration
}
