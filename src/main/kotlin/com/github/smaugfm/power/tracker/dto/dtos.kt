package com.github.smaugfm.power.tracker.dto

import com.github.smaugfm.power.tracker.persistence.TelegramChatIdEntity
import jakarta.persistence.OneToMany
import java.time.ZonedDateTime

data class Event(
    val id: EventId,
    val state: Boolean,
    val type: EventType,
    val configId: Long,
    val time: ZonedDateTime
)

typealias EventId = Long;

enum class EventType {
    POWER,
    ISP
}

data class PowerIspState(
    val hasPower: Boolean?,
    val hasIsp: Boolean?
)

data class Monitorable(
    val id: Long,
    val address: String,
    val port: Int?,
)
