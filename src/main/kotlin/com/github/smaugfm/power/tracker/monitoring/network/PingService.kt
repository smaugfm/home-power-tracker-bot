package com.github.smaugfm.power.tracker.monitoring.network

import com.github.smaugfm.power.tracker.dto.Monitorable
import com.github.smaugfm.power.tracker.dto.PowerIspState
import kotlinx.coroutines.CoroutineScope

interface PingService {
    suspend fun ping(scope: CoroutineScope, config: Monitorable): PowerIspState
}
