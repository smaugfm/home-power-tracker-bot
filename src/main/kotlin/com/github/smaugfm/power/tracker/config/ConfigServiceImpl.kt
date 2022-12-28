package com.github.smaugfm.power.tracker.config

import com.github.smaugfm.power.tracker.dto.Monitorable
import com.github.smaugfm.power.tracker.persistence.ConfigsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service

@Service
class ConfigServiceImpl(
    private val repository: ConfigsRepository
) : ConfigService {
    override suspend fun getAllMonitorable() =
        withContext(Dispatchers.IO) {
            repository.findAll().map {
                Monitorable(it.id, it.address, it.port)
            }.asFlow()
        }
}
