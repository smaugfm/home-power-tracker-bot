package com.github.smaugfm.power.tracker.config

import com.github.smaugfm.power.tracker.Config
import com.github.smaugfm.power.tracker.ConfigId
import kotlinx.coroutines.flow.Flow

interface ConfigService {
    suspend fun getAll(): Flow<Config>
    suspend fun getById(id: ConfigId): Config?
}
