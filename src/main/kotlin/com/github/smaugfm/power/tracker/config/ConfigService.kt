package com.github.smaugfm.power.tracker.config

import com.github.smaugfm.power.tracker.Config
import kotlinx.coroutines.flow.Flow

interface ConfigService {
    suspend fun getAll(): Flow<Config>
}
