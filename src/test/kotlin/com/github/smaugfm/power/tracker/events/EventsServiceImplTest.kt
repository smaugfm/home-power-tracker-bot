package com.github.smaugfm.power.tracker.events

import assertk.all
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThanOrEqualTo
import com.github.smaugfm.power.tracker.PowerIspState
import com.github.smaugfm.power.tracker.RepositoryTestBase
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant

class EventsServiceImplTest : RepositoryTestBase() {

    @Autowired
    private lateinit var service: EventsService

    @Test
    fun getLaterEventsTest() {
        val configId = saveConfig1().id
        val configId2 = saveConfig2().id

        db.sql(
            """
            insert into tb_events(config_id, type, state, created, id) 
                values (${configId}, 'POWER', false, '2022-12-25 00:00:00 Europe/Kiev', 1);
            insert into tb_events(config_id, type, state, created, id) 
                values (${configId}, 'POWER', true , '2022-12-26 00:00:00 Europe/Kiev', 2);
            insert into tb_events(config_id, type, state, created, id)
                values (${configId}, 'POWER', false, '2022-12-27 00:00:00 Europe/Kiev', 3);
            insert into tb_events(config_id, type, state, created, id)
                values (${configId2}, 'POWER', true, '2022-12-28 00:00:00 Europe/Kiev', 4);
            insert into tb_events(config_id, type, state, created, id)
                values (${configId}, 'ISP', false, '2022-12-29 00:00:00 Europe/Kiev', 5);
            insert into tb_events(config_id, type, state, created, id)
                values (${configId2}, 'POWER', true, '2022-12-29 01:00:00 Europe/Kiev', 6);
            insert into tb_events(config_id, type, state, created, id)
                values (${configId}, 'ISP', true, '2022-12-30 00:00:00 Europe/Kiev', 7);
            """.trimIndent()
        ).then().block()

        val event = runBlocking { service.getEvent(4L) }!!
        val result = runBlocking {
            service.getEventsAfter(event.configId, event.time).toList()
        }
        assertThat(result).hasSize(2)
        assertThat(result[0].id).isEqualTo(4L)
        assertThat(result[1].id).isEqualTo(6L)
    }

    @Test
    fun calculateLaterEventsTest() {
        val configId = saveConfig1().id

        val prev = PowerIspState(hasPower = null, hasIsp = null)
        val next = PowerIspState(hasPower = true, hasIsp = true)

        val now = Instant.now()
        val events = runBlocking { service.calculateAddEvents(prev, next, configId).toList() }
        assertThat(events).hasSize(2)
        assertThat(events[0].id).isEqualTo(1)
        assertThat(events[0].state).isEqualTo(true)
        assertThat(events[0].configId).isEqualTo(configId)
        assertThat(events[0].time).isGreaterThanOrEqualTo(now)
        assertThat(events[1].id).isEqualTo(2)
        assertThat(events[1].state).isEqualTo(true)
        assertThat(events[1].configId).isEqualTo(configId)
        assertThat(events[1].time).all {
            isGreaterThanOrEqualTo(now)
            isGreaterThanOrEqualTo(events[0].time)
        }
        val other = runBlocking { service.findAllEvents(configId).toList() }
        assertThat(other).isEqualTo(events)
    }

    @Test
    fun eventsFindCurrentState() {
        val configId = saveConfig1().id
        val configId2 = saveConfig2().id

        assertThat(
            runBlocking { service.getCurrentState(configId) }
        ).isEqualTo(PowerIspState(null, null))
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

        var result = runBlocking { service.getCurrentState(configId) }
        assertThat(result).isEqualTo(PowerIspState(hasPower = false, hasIsp = true))
        result = runBlocking { service.getCurrentState(configId2) }
        assertThat(result).isEqualTo(PowerIspState(hasPower = true, hasIsp = null))
    }

}
