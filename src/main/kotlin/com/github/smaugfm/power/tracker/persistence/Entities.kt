package com.github.smaugfm.power.tracker.persistence

import com.github.smaugfm.power.tracker.events.EventType
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import org.springframework.data.annotation.CreatedDate
import java.time.ZonedDateTime

@Entity
@Table(name = "tb_configs")
class ConfigEntity(
    @NotBlank
    @Column(nullable = false)
    val address: String,
    @Column(nullable = false)
    val port: Short,
    @Column(name = "notify_power")
    val notifyPower: Boolean,
    @Column(name = "notify_isp")
    val notifyIsp: Boolean,
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sq_tb_configs")
    @SequenceGenerator(name = "sq_tb_configs", sequenceName = "sq_tb_configs", allocationSize = 1)
    val id: Long = 0,
) {
    @OneToMany(mappedBy = "config")
    lateinit var telegramChatIds: Set<TelegramChatIdEntity>
}

@Entity
@Table(name = "tb_telegram_chat_ids")
class TelegramChatIdEntity(
    @Id
    @Column(name = "chat_id")
    val chatId: Int,
) {
    @ManyToOne(cascade = [CascadeType.ALL], optional = false)
    @JoinColumn(name = "config_id")
    lateinit var config: ConfigEntity
}

@Entity
@Table(name = "tb_events")
class EventEntity(
    @Column(nullable = false)
    val state: Boolean,
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    val type: EventType,
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sq_tb_events")
    @SequenceGenerator(name = "sq_tb_events", sequenceName = "sq_tb_events", allocationSize = 1)
    val id: Long = 0,
) {
    @ManyToOne(cascade = [CascadeType.ALL], optional = false)
    @JoinColumn(name = "config_id")
    lateinit var config: ConfigEntity

    @CreatedDate
    lateinit var created: ZonedDateTime
}

