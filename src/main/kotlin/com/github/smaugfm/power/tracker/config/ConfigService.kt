package com.github.smaugfm.power.tracker.config

import com.github.smaugfm.power.tracker.Config
import com.github.smaugfm.power.tracker.ConfigId
import com.github.smaugfm.power.tracker.NewConfig
import com.github.smaugfm.power.tracker.YasnoGroup
import kotlinx.coroutines.flow.Flow

interface ConfigService {
    suspend fun getAll(): Flow<Config>
    suspend fun getYasnoGroup(id: ConfigId): YasnoGroup?

    suspend fun getById(configId: ConfigId): Config?
    suspend fun getByAddress(address: String): Config?
    suspend fun addNewConfig(newConfig: NewConfig): Config
}
