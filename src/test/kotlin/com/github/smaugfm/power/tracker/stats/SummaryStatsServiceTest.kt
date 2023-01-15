package com.github.smaugfm.power.tracker.stats

import assertk.assertThat
import assertk.assertions.*
import com.github.smaugfm.power.tracker.Event
import com.github.smaugfm.power.tracker.EventType
import com.github.smaugfm.power.tracker.RepositoryTestBase
import com.github.smaugfm.power.tracker.SummaryStatsPeriod
import com.github.smaugfm.power.tracker.stats.summary.SummaryStatsService
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration
import java.time.OffsetDateTime

class SummaryStatsServiceTest : RepositoryTestBase() {
    @Autowired
    private lateinit var service: SummaryStatsService

    @Test
    fun determinePeriodForEventTest_1() {
        val configId = saveConfig1().id

        insertEventWithTime(configId, "2022-12-01 00:00:00 Europe/Kiev")
        val (year, month) = getPeriods(configId, "2023-01-01T12:00:00.000+02:00")
        assertThat(year.first).isInstanceOf(SummaryStatsPeriod.LastYear::class)
        assertThat(month.first).isInstanceOf(SummaryStatsPeriod.LastMonth::class)
        assertThat(year.second).assertThat(
            OffsetDateTime.parse("2022-01-01T00:00:00.000+02:00").toInstant(),
        )
        assertThat(month.second).assertThat(
            OffsetDateTime.parse("2022-01-01T00:00:00.000+02:00").toInstant(),
        )
    }

    @Test
    fun determinePeriodForEventTest_2() {
        val configId = saveConfig1().id

        insertEventWithTime(configId, "2021-12-29 00:00:00 Europe/Kiev")
        assertThat(getPeriods(configId, "2021-12-31T23:59:59.999+02:00")).isEmpty()
    }

    @Test
    fun determinePeriodForEventTest_4() {
        val configId = saveConfig1().id

        insertEventWithTime(configId, "2023-01-31 23:59:59 Europe/Kiev")
        val (period, to) = getPeriods(configId, "2023-02-06T00:00:00.000+02:00")
        assertThat(period).isInstanceOf(SummaryStatsPeriod.LastMonth::class)
        assertThat(to).assertThat(
            OffsetDateTime.parse("2023-01-02T00:00:00.000+02:00").toInstant(),
        )
    }

    @Test
    fun calculateForPeriodTest() {
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
                SummaryStatsPeriod.LastMonth
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

    private fun getPeriods(configId: Long, time: String) =
        runBlocking {
            service.determinePeriodForEvent(
                Event(
                    2,
                    false,
                    EventType.POWER,
                    configId,
                    OffsetDateTime.parse(time).toInstant(),
                )
            )
        }

    private fun insertEventWithTime(configId: Long, time: String) {
        db.sql(
            """
                insert into tb_events(config_id, type, state, created)
                    values (${configId}, 'POWER', true, '${time}');
                """.trimIndent()
        ).then().block()
    }
}
