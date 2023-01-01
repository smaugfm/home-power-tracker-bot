package com.github.smaugfm.power.tracker.stats

import com.github.smaugfm.power.tracker.dto.Event
import kotlinx.coroutines.flow.Flow

interface StatsService {
    suspend fun calculateEventStats(event: Event): List<EventStats>
}
