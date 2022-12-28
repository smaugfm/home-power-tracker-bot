package com.github.smaugfm.power.tracker.network

import com.github.smaugfm.power.tracker.dto.Monitorable
import com.github.smaugfm.power.tracker.dto.PowerIspState
import java.time.Duration

interface PingService {
    suspend fun ping(config: Monitorable): PowerIspState
}
