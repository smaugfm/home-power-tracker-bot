package com.github.smaugfm.power.tracker.monitoring.network

import com.github.smaugfm.power.tracker.dto.Monitorable
import com.github.smaugfm.power.tracker.dto.PowerIspState
import java.time.Duration

interface PingService {
    suspend fun ping(config: Monitorable): PowerIspState
}
