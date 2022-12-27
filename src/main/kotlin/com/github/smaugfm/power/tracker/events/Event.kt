package com.github.smaugfm.power.tracker.events

import java.time.ZonedDateTime

data class Event(
    val id: EventId,
    val state: Boolean,
    val type: EventType,
    val configId: Long,
    val time: ZonedDateTime
)

data class NewEvent(
    val state: Boolean,
    val type: EventType,
    val configId: Long,
    val time: ZonedDateTime
)
