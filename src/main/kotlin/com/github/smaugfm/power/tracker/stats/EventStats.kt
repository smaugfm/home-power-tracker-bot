package com.github.smaugfm.power.tracker.stats

import com.github.smaugfm.power.tracker.dto.EventSummaryType
import com.github.smaugfm.power.tracker.dto.EventType
import java.time.Duration

sealed class EventStats {
    sealed class Single(
        open val state: Boolean,
        open val type: EventType,
        override val lastInverse: Duration
    ) : EventStats(), LastInverseStats {
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

    class Summary(
        val summaryType: EventSummaryType,
        val upTotal: Duration,
        val downTotal: Duration,
        val upPercent: Double,
    ) : EventStats()

    interface LastInverseStats {
        val lastInverse: Duration
    }
}
