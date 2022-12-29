package com.github.smaugfm.power.tracker.persistence

import assertk.assertThat
import assertk.assertions.*
import com.github.smaugfm.power.tracker.ResetDatabaseTestExecutionListener
import com.github.smaugfm.power.tracker.TestBase
import com.github.smaugfm.power.tracker.dto.EventType
import org.h2.tools.DeleteDbFiles
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.test.context.TestExecutionListeners
import java.time.ZonedDateTime

@TestExecutionListeners(
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS,
    listeners = [ResetDatabaseTestExecutionListener::class]
)
class RepositoryTest : TestBase() {
    @Autowired
    private lateinit var configRepository: ConfigsRepository

    @Autowired
    private lateinit var eventsRepository: EventsRepository

    @Autowired
    private lateinit var db: DatabaseClient

    companion object {
        @BeforeAll
        @JvmStatic
        fun deleteH2() {
            DeleteDbFiles.main()
        }
    }

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
        val now = ZonedDateTime.now()
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

    @Test
    fun eventsFindCurrentState() {
        val configId = configRepository.save(
            ConfigEntity(
                "vasa.com",
                8080,
            )
        ).block()!!.id
        val configId2 = configRepository.save(
            ConfigEntity(
                "other.com",
                8080,
            )
        ).block()!!.id

        assertThat(
            eventsRepository.findTop1ByConfigIdAndTypeOrderByCreatedDesc(
                configId,
                EventType.POWER
            ).block()
        ).isNull()
        db.sql(
            """
            insert into tb_events(config_id, type, state, created) 
                values (${configId}, 'POWER', false, '2022-12-25 00:00:00 Europe/Kiev');
            insert into tb_events(config_id, type, state, created) 
                values (${configId}, 'POWER', true , '2022-12-26 00:00:00 Europe/Kiev');
            insert into tb_events(config_id, type, state, created) 
                values (${configId}, 'POWER', false, '2022-12-27 00:00:00 Europe/Kiev');
            insert into tb_events(config_id, type, state, created) 
                values (${configId2}, 'POWER', true, '2022-12-28 00:00:00 Europe/Kiev');
            insert into tb_events(config_id, type, state, created) 
                values (${configId}, 'ISP', false, '2022-12-29 00:00:00 Europe/Kiev');
            insert into tb_events(config_id, type, state, created) 
                values (${configId}, 'ISP', true, '2022-12-30 00:00:00 Europe/Kiev');
            """.trimIndent()
        ).then().block()
        val date = ZonedDateTime.parse(
            "2022-12-27T00:00:00+02:00"
        )

        val result = eventsRepository.findTop1ByConfigIdAndTypeOrderByCreatedDesc(
            configId,
            EventType.POWER
        ).block()!!
        assertThat(result.created).isEqualTo(date)
    }
}
