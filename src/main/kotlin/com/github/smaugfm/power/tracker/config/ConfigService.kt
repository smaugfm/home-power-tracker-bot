package com.github.smaugfm.power.tracker.config

import com.github.smaugfm.power.tracker.Config
import com.github.smaugfm.power.tracker.ConfigId
import com.github.smaugfm.power.tracker.YasnoGroup
import kotlinx.coroutines.flow.Flow
import reactor.core.publisher.Flux

interface ConfigService {
    suspend fun getAll(): Flux<Config>
    suspend fun getYasnoGroup(id: ConfigId): YasnoGroup?
}
