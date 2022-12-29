package com.github.smaugfm.power.tracker.dto

import java.time.ZonedDateTime

data class Event(
    val id: EventId,
    val state: Boolean,
    val type: EventType,
    val configId: ConfigId,
    val time: ZonedDateTime
)

typealias EventId = Long;
typealias ConfigId = Long;
typealias MessageId = dev.inmo.tgbotapi.types.MessageId

enum class EventType {
    POWER,
    ISP
}

data class PowerIspState(
    val hasPower: Boolean?,
    val hasIsp: Boolean?
)

data class Monitorable(
    val id: ConfigId,
    val address: String,
    val port: Int?,
)
