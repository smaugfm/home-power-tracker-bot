package com.github.smaugfm.power.tracker.stats

import com.github.smaugfm.power.tracker.EventType
import com.github.smaugfm.power.tracker.PeriodicStats
import com.github.smaugfm.power.tracker.SummaryStatsPeriod
import java.time.Duration

sealed class EventStats(
    open val type: EventType,
) {
    sealed class Single(
        open val state: Boolean,
        type: EventType,
    ) : EventStats(type) {
        data class First(
            override val state: Boolean,
            override val type: EventType
        ) : Single(state, type)

        sealed class Consecutive(
            override val state: Boolean,
            override val type: EventType,
            open val lastInverse: Duration
        ): Single(state, type) {
            data class Other(
                override val state: Boolean,
                override val type: EventType,
                override val lastInverse: Duration
            ) : Consecutive(state, type, lastInverse)

            data class IspDown(
                val lastUPSCharge: Duration?,
                val lastUPSOperation: Duration?,
                override val lastInverse: Duration
            ) : Consecutive(false, EventType.ISP, lastInverse)
        }
    }

    data class Summary(
        override val type: EventType,
        val period: SummaryStatsPeriod,
        val turnOffCount: Int,
        val upTotal: Duration,
        val downTotal: Duration,
        val upPercent: Double,
        val upPeriodicStats: PeriodicStats,
        val downPeriodicStats: PeriodicStats
    ) : EventStats(type)

    class LastWeekPowerScheduleImage(
        val pngBytes: ByteArray
    ) : EventStats(EventType.POWER)
}
