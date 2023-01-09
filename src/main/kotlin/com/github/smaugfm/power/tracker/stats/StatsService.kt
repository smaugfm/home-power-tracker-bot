package com.github.smaugfm.power.tracker.stats

import com.github.smaugfm.power.tracker.Event

interface StatsService {
    suspend fun calculateEventStats(event: Event): List<EventStats>
}
