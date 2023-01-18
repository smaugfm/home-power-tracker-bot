package com.github.smaugfm.power.tracker.scripts

import com.beust.klaxon.Klaxon
import com.github.smaugfm.power.tracker.persistence.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.r2dbc.core.DatabaseClient
import reactor.core.publisher.Flux
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Disabled
class MigrationScript : ScriptBase() {
    @Autowired
    private lateinit var configs: ConfigsRepository

    @Autowired
    private lateinit var db: DatabaseClient

    @Autowired
    private lateinit var chatIds: TelegramChatIdsRepository
    private val format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSX")

    @Test
    fun migrateOldDb() {
        val result = getOldDb()

        val pairs = Flux.fromIterable(result)
            .flatMap { config ->
                configs.save(
                    ConfigEntity(
                        config.host.host,
                        config.host.port,
                        config.notificationSettings.power,
                        config.notificationSettings.power
                    )
                ).map {
                    it to config
                }
            }.collectList()
            .block()!!
        Flux.fromIterable(pairs)
            .flatMap { (configEntity, config) ->
                Flux.fromIterable(config.telegramChatIds)
                    .flatMap { telegramChatId ->
                        chatIds.save(
                            TelegramChatIdEntity(
                                telegramChatId,
                                configEntity.id,
                            ).markNew()
                        )
                    }
            }.collectList()
            .block()
        Flux.fromIterable(pairs)
            .flatMap { (configEntity, config) ->
                Flux.fromIterable(config.events)
                    .index()
                    .flatMap { eventTuple ->
                        val event = eventTuple.t2
                        val parsedTime = ZonedDateTime.parse(event.time)
                        val psqlTime = parsedTime.format(format)
                        db.sql(
                            """
                    insert into tb_events(config_id, type, state, created)
                        values (${configEntity.id}, '${event.type.uppercase()}', ${event.state}, '${psqlTime}')
                                        """.trimIndent()
                        ).then()
                    }
            }.collectList()
            .block()
    }

    data class OldDbEvent(
        val type: String,
        val state: Boolean,
        val time: String
    )

    data class OldDbHost(
        val host: String,
        val port: Int? = null
    )

    data class OldDbNotificationSettings(
        val power: Boolean,
        val isp: Boolean
    )

    data class OldDbConfig(
        val host: OldDbHost,
        val telegramChatIds: List<Long>,
        val events: List<OldDbEvent>,
        val notificationSettings: OldDbNotificationSettings
    )

    private fun getOldDb() =
        Klaxon()
            .parseArray<OldDbConfig>(
                MigrationScript::javaClass
                    .javaClass
                    .classLoader
                    .getResource("db.json")
                    ?.readText()!!
            )!!
}
