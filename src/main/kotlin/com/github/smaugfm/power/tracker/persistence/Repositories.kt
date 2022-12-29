package com.github.smaugfm.power.tracker.persistence

import com.github.smaugfm.power.tracker.dto.EventType
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.r2dbc.repository.R2dbcRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface ConfigsRepository : R2dbcRepository<ConfigEntity, Long> {
}

interface EventsRepository : R2dbcRepository<EventEntity, Long> {
    fun findTop1ByConfigIdAndTypeOrderByCreatedDesc(
        configId: Long,
        eventType: EventType
    ): Mono<EventEntity>

    fun findAllByConfigId(configId: Long): Flux<EventEntity>
}

interface TelegramChatIdsRepository : R2dbcRepository<TelegramChatIdEntity, Long> {
    fun findAllByConfigId(configId: Long): Flux<TelegramChatIdEntity>
}

interface TelegramMessagesRepository : R2dbcRepository<TelegramMessageEntity, Long> {
}
