package com.github.smaugfm.power.tracker.network

import com.github.smaugfm.power.tracker.Config
import com.github.smaugfm.power.tracker.PowerIspState
import kotlinx.coroutines.CoroutineScope

interface PingService {
    suspend fun ping(scope: CoroutineScope, config: Config): PowerIspState
}
