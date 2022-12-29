package com.github.smaugfm.power.tracker.config

import com.github.smaugfm.power.tracker.dto.ConfigId
import com.github.smaugfm.power.tracker.dto.EventId
import com.github.smaugfm.power.tracker.dto.Monitorable
import kotlinx.coroutines.flow.Flow

interface ConfigService {
    suspend fun getAllMonitorable(): Flow<Monitorable>
}
