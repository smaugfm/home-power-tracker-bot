package com.github.smaugfm.power.tracker

import kotlinx.coroutines.CompletableDeferred
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

data class Event(
    val id: EventId,
    val state: Boolean,
    val type: EventType,
    val configId: ConfigId,
    val time: Instant
) {
    fun since(earlier: Event): Duration = Duration.between(earlier.time, time)
}

data class NewEvent(
    val state: Boolean,
    val type: EventType,
    val configId: ConfigId,
)

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

data class MonitoringEvent(
    val prevState: PowerIspState,
    val curState: PowerIspState,
    val configId: ConfigId
)

data class Config(
    val id: ConfigId,
    val address: String,
    val yasnoGroup: YasnoGroup,
    val port: Int?,
)

sealed class SummaryStatsPeriod {
    object LastWeek : SummaryStatsPeriod()
    object LastMonth : SummaryStatsPeriod()
    object LastYear : SummaryStatsPeriod()
}

data class PeriodicStats(
    val state: Boolean,
    val longestPeriod: Duration,
    val shortestPeriod: Duration,
    val medianPeriod: Duration
)

sealed class UserInteractionData(
    open val configId: ConfigId
) {
    data class TelegramUserInteractionData(
        override val configId: ConfigId,
        val messageId: Long,
        val telegramChatId: Long,
    ) : UserInteractionData(configId)

    data class Noop(override val configId: ConfigId) : UserInteractionData(configId)
}

data class EventDeletionRequest<T : UserInteractionData>(
    val data: T,
    val eventId: EventId
)

enum class YasnoGroup {
    Group1, Group2
}

data class ScheduleImageCreateRequest(
    val group: YasnoGroup,
    val outageHourRanges: List<IntRange>,
    val mondayDate: LocalDate,
    val future: CompletableDeferred<ByteArray>
)
