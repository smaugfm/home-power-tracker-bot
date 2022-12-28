package com.github.smaugfm.power.tracker.persistence

import com.github.smaugfm.power.tracker.dto.EventType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ConfigsRepository : JpaRepository<ConfigEntity, Long> {
}

interface EventsRepository : JpaRepository<EventEntity, Long> {
    @Query(
        "select e.state from EventEntity e where e.config.id = ?1 and e.type = ?2 " +
                "order by e.created desc limit 1"
    )
    fun findCurrentState(configId: Long, type: EventType): Boolean?
}

interface TelegramChatIdsRepository : JpaRepository<TelegramChatIdEntity, Short> {

}
