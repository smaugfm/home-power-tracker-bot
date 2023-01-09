package com.github.smaugfm.power.tracker.persistence

import com.github.smaugfm.power.tracker.EventType
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.r2dbc.repository.R2dbcRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant

interface ConfigsRepository : R2dbcRepository<ConfigEntity, Long>

interface EventsRepository : R2dbcRepository<EventEntity, Long> {
    fun findTop1ByConfigIdAndTypeOrderByCreatedDesc(
        configId: Long,
        eventType: EventType
    ): Mono<EventEntity>

    fun findAllByConfigId(configId: Long): Flux<EventEntity>

    @Query(
        "select * from tb_events " +
                "where config_id = $1 " +
                "and created < $2 " +
                "and ($3 IS NULL or state = $3)" +
                "and ($4 IS NULL or type = $4)" +
                "order by created desc limit 1"
    )
    fun findFirstPreviousLike(
        configId: Long,
        before: Instant,
        state: Boolean?,
        type: EventType?,
    ): Mono<EventEntity>

    fun findAllByConfigIdAndCreatedIsGreaterThanEqualOrderByCreatedAsc(
        configId: Long,
        after: Instant
    ): Flux<EventEntity>
}

interface TelegramChatIdsRepository : R2dbcRepository<TelegramChatIdEntity, Long> {
    fun findAllByConfigId(configId: Long): Flux<TelegramChatIdEntity>
}

interface TelegramMessagesRepository : R2dbcRepository<TelegramMessageEntity, Long> {
    fun findByMessageIdAndChatId(messageId: Long, chatId: Long): Mono<TelegramMessageEntity>
    fun findAllByEventId(eventId: Long): Flux<TelegramMessageEntity>
}
