package com.github.smaugfm.power.tracker.network

import com.github.smaugfm.power.tracker.events.NewEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.springframework.stereotype.Service

@Service
class PingService {
    fun ping(): Flow<NewEvent> {
        return emptyFlow()
    }
}
