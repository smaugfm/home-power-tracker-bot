package com.github.smaugfm.power.tracker.persistence

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isEqualToWithGivenProperties
import assertk.assertions.isGreaterThanOrEqualTo
import com.github.smaugfm.power.tracker.RepositoryTestBase
import com.github.smaugfm.power.tracker.EventType
import org.junit.jupiter.api.Test
import java.time.Instant

class RepositoriesTest : RepositoryTestBase() {

    @Test
    fun configSaveTest() {
        val saved = saveConfig1()

        assertThat(saved).isEqualToWithGivenProperties(
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
        val c = saveConfig1()
        val e = EventEntity(false, EventType.ISP, c.id)
        val saved = eventsRepository.save(e).block()

        assertThat(saved!!.id).isEqualTo(1L)
        assertThat(saved.configId).isEqualTo(e.configId)
        assertThat(saved.created).isGreaterThanOrEqualTo(now)
        assertThat(saved.type).isEqualTo(e.type)
        assertThat(saved.state).isEqualTo(e.state)
    }
}
