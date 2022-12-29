package com.github.smaugfm.power.tracker.persistence

import com.github.smaugfm.power.tracker.dto.EventType
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.r2dbc.repository.R2dbcRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface ConfigsRepository : R2dbcRepository<ConfigEntity, Long> {
}

interface EventsRepository : R2dbcRepository<EventEntity, Long> {
    @Query(
        "select e.state from EventEntity e where e.config.id = ?1 and e.type = ?2 " +
                "order by e.created desc limit 1"
    )
    fun findCurrentState(configId: Long, type: EventType): Mono<Boolean>

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
