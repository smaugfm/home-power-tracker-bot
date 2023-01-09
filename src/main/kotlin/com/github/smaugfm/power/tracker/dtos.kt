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

enum class EventSummaryType {
    DAY,
    WEEK,
    MONTH
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
