package com.github.smaugfm.power.tracker.stats

import assertk.assertThat
import assertk.assertions.containsExactly
import com.github.smaugfm.power.tracker.EventType
import com.github.smaugfm.power.tracker.RepositoryTestBase
import com.github.smaugfm.power.tracker.events.EventsService
import com.github.smaugfm.power.tracker.stats.single.SingleEventStatsService
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration

class StatsServiceImplTest : RepositoryTestBase() {

    @Autowired
    private lateinit var service: SingleEventStatsService

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
                values (${configId}, 'POWER', true, '2022-12-25 00:01:00 Europe/Kiev', 2);
            insert into tb_events(config_id, type, state, created, id) 
                values (${configId}, 'POWER', false , '2022-12-26 00:00:00 Europe/Kiev', 3);
            insert into tb_events(config_id, type, state, created, id)
                values (${configId}, 'ISP', false, '2022-12-29 04:00:00 Europe/Kiev', 4);
            """.trimIndent()
        ).then().block()

        val events = runBlocking { eventsService.findAllEvents(configId).toList() }

        val get = { i: Int -> events[i - 1] }

        assertThat(runBlocking {
            service.calculate(get(1))
        }).containsExactly(EventStats.Single.First(true, EventType.ISP))

        assertThat(runBlocking {
            service.calculate(get(2))
        }).containsExactly(EventStats.Single.First(true, EventType.POWER))

        assertThat(runBlocking {
            service.calculate(get(3))
        }).containsExactly(
            EventStats.Single.Consecutive.Other(
                false,
                EventType.POWER,
                Duration.ofDays(1).minusMinutes(1)
            )
        )

        assertThat(runBlocking {
            service.calculate(get(4))
        }).containsExactly(
            EventStats.Single.Consecutive.IspDown(
                Duration.ofDays(1).minusMinutes(1),
                Duration.ofDays(3).plusHours(4),
                Duration.ofDays(4).plusHours(4),
            )
        )
    }
}
