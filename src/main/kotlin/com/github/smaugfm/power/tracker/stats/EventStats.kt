package com.github.smaugfm.power.tracker.stats

import com.github.smaugfm.power.tracker.EventType
import com.github.smaugfm.power.tracker.LastInverseStats
import com.github.smaugfm.power.tracker.PeriodicStats
import com.github.smaugfm.power.tracker.SummaryStatsPeriod
import java.time.Duration

sealed class EventStats(
    open val type: EventType,
) {
    sealed class Single(
        open val state: Boolean,
        type: EventType,
        override val lastInverse: Duration
    ) : EventStats(type), LastInverseStats {
        data class LastInverseOnly(
            override val state: Boolean,
            override val type: EventType,
            override val lastInverse: Duration
        ) : Single(state, type, lastInverse)

        data class IspDownStats(
            val lastUPSCharge: Duration?,
            val lastUPSOperation: Duration?,
            override val lastInverse: Duration
        ) : Single(false, EventType.ISP, lastInverse)
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

}
