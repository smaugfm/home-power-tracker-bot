package com.github.smaugfm.power.tracker.persistence

import com.github.smaugfm.power.tracker.EventType
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table(name = "tb_configs")
data class ConfigEntity(
    @Column
    val address: String,
    val port: Int?,
    @Column("notify_power")
    val notifyPower: Boolean = true,
    @Column("notify_isp")
    val notifyIsp: Boolean = true,
    @Column("yasno_group")
    val yasnoGroup: Int? = null,
    @Id
    var id: Long = 0,
)

@Table(name = "tb_events")
data class EventEntity(
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
    lateinit var created: Instant
}

@Table(name = "tb_initial_events")
data class InitialEventEntity(
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
    lateinit var created: Instant
}

@Table(name = "tb_telegram_chat_ids")
data class TelegramChatIdEntity(
    @Id
    @Column("chat_id")
    val chatId: Long,
    @Column("config_id")
    val configId: Long,
) : Persistable<Long> {
    @Transient
    private var isNewField = false

    fun markNew() = this.also {
        it.isNewField = true
    }

    override fun getId() = chatId
    override fun isNew() = isNewField
}

@Table(name = "tb_telegram_messages")
data class TelegramMessageEntity(
    @Column("message_id")
    val messageId: Long,
    @Column("chat_id")
    val chatId: Long,
    @Column("event_id")
    val eventId: Long,
    @Id
    var id: Long = 0,
)
