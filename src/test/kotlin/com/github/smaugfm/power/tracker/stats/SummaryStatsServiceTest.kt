package com.github.smaugfm.power.tracker.stats

import assertk.assertThat
import assertk.assertions.isBetween
import assertk.assertions.isEqualTo
import com.github.smaugfm.power.tracker.EventType
import com.github.smaugfm.power.tracker.RepositoryTestBase
import com.github.smaugfm.power.tracker.SummaryStatsPeriod
import com.github.smaugfm.power.tracker.events.EventsService
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration
import java.time.OffsetDateTime

class SummaryStatsServiceTest : RepositoryTestBase() {
    @Autowired
    private lateinit var service: SummaryStatsService

    @Test
    fun test() {
        val configId = saveConfig1().id

        db.sql(
            """
            insert into tb_events(config_id, type, state, created, id)
                values (${configId}, 'POWER', true, '2022-12-01 00:00:00 Europe/Kiev', 1);
            insert into tb_events(config_id, type, state, created, id) 
                values (${configId}, 'POWER', true, '2022-12-01 04:00:00 Europe/Kiev', 2);
            insert into tb_events(config_id, type, state, created, id) 
                values (${configId}, 'POWER', false , '2022-12-01 08:00:00 Europe/Kiev', 3);
            insert into tb_events(config_id, type, state, created, id)
                values (${configId}, 'POWER', true, '2022-12-01 16:00:00 Europe/Kiev', 4);
            insert into tb_events(config_id, type, state, created, id)
                values (${configId}, 'POWER', false, '2022-12-02 00:00:00 Europe/Kiev', 5);
            """.trimIndent()
        ).then().block()

        with(runBlocking {
            service.calculateForPeriod(
                configId,
                EventType.POWER,
                OffsetDateTime.parse("2022-12-02T12:00:00.000+02:00").toInstant(),
                SummaryStatsPeriod.Month
            )
        }!!) {
            assertThat(upTotal).isEqualTo(Duration.ofHours(16))
            assertThat(downTotal).isEqualTo(Duration.ofHours(20))
            assertThat(upPercent).isBetween(44.4, 44.5)
            with(upPeriodicStats) {
                assertThat(longestPeriod).isEqualTo(Duration.ofHours(8))
                assertThat(shortestPeriod).isEqualTo(Duration.ofHours(4))
                assertThat(medianPeriod).isEqualTo(Duration.ofHours(4))
            }
            println("\n")
            with(downPeriodicStats) {
                assertThat(longestPeriod).isEqualTo(Duration.ofHours(12))
                assertThat(shortestPeriod).isEqualTo(Duration.ofHours(8))
                assertThat(medianPeriod).isEqualTo(Duration.ofHours(10))
            }
        }
    }
}
