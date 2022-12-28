package com.github.smaugfm.power.tracker.stats

import com.github.smaugfm.power.tracker.dto.Event
import kotlinx.coroutines.flow.Flow
import org.springframework.stereotype.Service

@Service
class StatsServiceImpl : StatsService {
    override fun calculateEventStats(event: Event): Flow<EventStats> {
        TODO("Not yet implemented")
    }
}
