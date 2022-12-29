package com.github.smaugfm.power.tracker.persistence

import com.github.smaugfm.power.tracker.dto.EventType
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.ZonedDateTime

@Table(name = "tb_configs")
class ConfigEntity(
    @Column
    val address: String,
    val port: Int?,
    @Column("notify_power")
    val notifyPower: Boolean = true,
    @Column("notify_isp")
    val notifyIsp: Boolean = true,
    @Id
    var id: Long = 0,
)

@Table(name = "tb_events")
class EventEntity(
    @Column
    val state: Boolean,
    @Column
    val type: EventType,
    @Column("config_id")
    val configId: Long,
    @Id
    var id: Long = 0,
) {
    @CreatedDate
    lateinit var created: ZonedDateTime
}

@Table(name = "tb_telegram_chat_ids")
class TelegramChatIdEntity(
    @Column("config_id")
    val configId: Long,
    @Id
    @Column("chat_id")
    var chatId: Long,
)

@Table(name = "tb_telegram_messages")
class TelegramMessageEntity(
    @Column("message_id")
    val messageId: Long,
    @Column("chat_id")
    val chatId: Long,
    @Column("event_id")
    val eventId: Long,
    @Id
    var id: Long = 0,
)
