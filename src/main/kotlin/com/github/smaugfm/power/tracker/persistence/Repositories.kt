package com.github.smaugfm.power.tracker.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface ConfigsRepository : JpaRepository<ConfigEntity, Long> {
}

interface EventsRepository : JpaRepository<EventEntity, Long> {

}

interface TelegramChatIdsRepository : JpaRepository<TelegramChatIdEntity, Short> {

}
