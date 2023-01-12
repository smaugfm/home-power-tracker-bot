package com.github.smaugfm.power.tracker.config

import com.github.smaugfm.power.tracker.Config
import com.github.smaugfm.power.tracker.ConfigId
import com.github.smaugfm.power.tracker.persistence.ConfigEntity
import com.github.smaugfm.power.tracker.persistence.ConfigsRepository
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service

@Service
class ConfigServiceImpl(
    private val repository: ConfigsRepository
) : ConfigService {
    override suspend fun getAll() =
        repository.findAll().asFlow().map(::mapDto)


    override suspend fun getById(id: ConfigId) =
        repository.findById(id).awaitSingleOrNull()?.let(::mapDto)

    private fun mapDto(it: ConfigEntity) = Config(it.id, it.address, it.port)
}
