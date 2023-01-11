package com.github.smaugfm.power.tracker.stats

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import com.github.smaugfm.power.tracker.RepositoryTestBase
import com.github.smaugfm.power.tracker.EventType
import com.github.smaugfm.power.tracker.events.EventsService
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration

class StatsServiceImplTest : RepositoryTestBase() {

    @Autowired
    private lateinit var service: StatsService

    @Autowired
    private lateinit var eventsService: EventsService

    @Test
    fun test() {
        val configId = saveConfig1().id

        db.sql(
            """
            insert into tb_events(config_id, type, state, created, id)
                values (${configId}, 'ISP', true, '2022-12-25 00:00:00 Europe/Kiev', 1);
            insert into tb_events(config_id, type, state, created, id) 
                values (${configId}, 'POWER', true, '2022-12-25 00:00:00 Europe/Kiev', 2);
            insert into tb_events(config_id, type, state, created, id) 
                values (${configId}, 'POWER', false , '2022-12-26 00:00:00 Europe/Kiev', 3);
            insert into tb_events(config_id, type, state, created, id)
                values (${configId}, 'ISP', false, '2022-12-29 04:00:00 Europe/Kiev', 4);
            """.trimIndent()
        ).then().block()

        val events = runBlocking { eventsService.findAllEvents(configId).toList() }

        val get = { i: Int -> events[i - 1] }

        assertThat(runBlocking {
            service.calculate(get(1)).toList()
        }).isEmpty()

        assertThat(runBlocking {
            service.calculate(get(2)).toList()
        }).isEmpty()

        val powerDown = runBlocking {
            service.calculate(get(3)).toList()
        }[0] as EventStats.Single.LastInverseOnly
        assertThat(powerDown.state).isFalse()
        assertThat(powerDown.type).isEqualTo(EventType.POWER)
        assertThat(powerDown.lastInverse).isEqualTo(Duration.ofDays(1))

        val ispDown = runBlocking {
            service.calculate(get(4)).toList()
        }[0] as EventStats.Single.IspDownStats
        assertThat(ispDown.state).isFalse()
        assertThat(ispDown.type).isEqualTo(EventType.ISP)
        assertThat(ispDown.lastInverse).isEqualTo(Duration.ofDays(4).plusHours(4))
        assertThat(ispDown.lastUPSCharge).isEqualTo(Duration.ofDays(1))
        assertThat(ispDown.lastUPSOperation).isEqualTo(Duration.ofDays(3).plusHours(4))
    }
}
