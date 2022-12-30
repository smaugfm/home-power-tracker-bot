package com.github.smaugfm.power.tracker.persistence

import assertk.assertThat
import assertk.assertions.*
import com.github.smaugfm.power.tracker.RepositoryTestBase
import com.github.smaugfm.power.tracker.dto.EventType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.r2dbc.core.DatabaseClient
import java.time.Instant
import java.time.ZonedDateTime

class RepositoriesTest : RepositoryTestBase() {
    @Autowired
    private lateinit var configRepository: ConfigsRepository

    @Autowired
    private lateinit var eventsRepository: EventsRepository

    @Test
    fun configSaveTest() {
        val e = ConfigEntity(
            "vasa.com",
            8080,
        )
        val saved = configRepository.save(e).block()

        assertThat(saved!!).isEqualToWithGivenProperties(
            ConfigEntity(
                "vasa.com",
                8080,
                true,
                true,
                1L
            ),
            ConfigEntity::address,
            ConfigEntity::port,
            ConfigEntity::notifyIsp,
            ConfigEntity::notifyPower,
            ConfigEntity::id,
        )
    }

    @Test
    fun eventsSaveTest() {
        val now = Instant.now()
        val c = ConfigEntity(
            "vasa.com",
            8080,
        )
        configRepository.save(c).block()
        val e = EventEntity(false, EventType.ISP, c.id)
        val saved = eventsRepository.save(e).block()

        assertThat(saved!!.id).isEqualTo(1L)
        assertThat(saved.configId).isEqualTo(e.configId)
        assertThat(saved.created).isGreaterThanOrEqualTo(now)
        assertThat(saved.type).isEqualTo(e.type)
        assertThat(saved.state).isEqualTo(e.state)
    }
}
