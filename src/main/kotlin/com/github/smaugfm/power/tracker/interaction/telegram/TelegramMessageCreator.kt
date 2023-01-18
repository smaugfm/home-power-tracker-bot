package com.github.smaugfm.power.tracker.interaction.telegram

import com.github.smaugfm.power.tracker.*
import com.github.smaugfm.power.tracker.stats.EventStats
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class TelegramMessageCreator {
    private val statsSeparator = "\n\n"

    fun getTelegramMessage(stats: List<EventStats>): String =
        stats.flatMap(this::createText).joinToString(statsSeparator)

    fun unstableNetworkMessage(duration: Duration) =
        "Мережа на сервері бота нестабільна останні ${duration.humanReadable()}"

    private fun createText(stats: EventStats): List<String> =
        when (stats) {
            is EventStats.Single ->
                listOfNotNull(
                    firstSingleEventText(stats),
                    (stats as? EventStats.Single.Consecutive)?.let(::consecutiveSingleEventText)
                )

            is EventStats.Summary -> summaryStatsText(stats)
            is EventStats.LastWeekPowerScheduleImage -> emptyList()
        }

    private fun firstSingleEventText(stats: EventStats.Single) =
        when (stats.type) {
            EventType.POWER -> "${if (stats.state) "🟢" else "🔴"} Світло " +
                    if (stats.state) "відновлено" else "зникло"

            EventType.ISP -> "${if (stats.state) "🟩" else "🟥"} Інтернет " +
                    if (stats.state) "з'явився" else "зник"
        }

    private fun consecutiveSingleEventText(stats: EventStats.Single.Consecutive) =
        when (stats) {
            is EventStats.Single.Consecutive.Other -> {
                if (stats.state)
                    "Скільки не було: " + stats.lastInverse.humanReadable()
                else
                    when (stats.type) {
                        EventType.POWER -> "Скільки трималось: "
                        EventType.ISP -> "Скільки тримався: "
                    } + stats.lastInverse.humanReadable()
            }

            is EventStats.Single.Consecutive.IspDown -> {
                val str =
                    StringBuilder("Скільки був: ${stats.lastInverse.humanReadable()}")
                if (stats.lastUPSOperation != null)
                    str.append("\nЗ цього, працював ДБЖ: ${stats.lastUPSOperation.humanReadable()}")
                if (stats.lastUPSCharge != null)
                    str.append(
                        "\nТривалість останньої зарядки акумуляторів ДБЖ: ${
                            stats.lastUPSCharge.humanReadable()
                        }"
                    )
                str.toString()
            }
        }

    private fun summaryStatsText(stats: EventStats.Summary): List<String> {
        val sb = StringBuilder("📊 Статистика по ")
        when (stats.type) {
            EventType.POWER -> sb.append("світлу")
            EventType.ISP -> sb.append("інтернету")
        }
        sb.append(" за ")
        sb.append(when (stats.period) {
            is SummaryStatsPeriod.Custom -> {
                stats.period.lastDays.let {
                    if (it == 1) "останній день"
                    else "останні ${Duration.ofDays(it.toLong()).humanReadable()}"
                }
            }

            SummaryStatsPeriod.LastMonth -> "останній місяць"
            SummaryStatsPeriod.LastWeek -> "останній тиждень"
            SummaryStatsPeriod.LastYear -> "останній рік"
        })
        sb.append(":\n\n")

        val was = when (stats.type) {
            EventType.POWER -> "було наявне"
            EventType.ISP -> "працював"
        }

        sb.append("Кількість відключень: ${stats.turnOffCount}\n\n")
        sb.append("Скільки часу всього $was: ${stats.upTotal.humanReadable()}\n")
        sb.append("Скільки часу всього не $was: ${stats.downTotal.humanReadable()}\n\n")

        sb.append("Процент часу скільки $was: ${Fmt.escape(stats.upPercent.format(0))}%\n\n")

        format(sb, stats.type, stats.upPeriodicStats)
        format(sb, stats.type, stats.downPeriodicStats)

        return listOf(sb.toString())
    }

    private fun format(sb: StringBuilder, type: EventType, periodicStats: PeriodicStats) {
        val byType = when (type) {
            EventType.POWER -> if (periodicStats.state) "зі світлом" else "без світла"
            EventType.ISP -> if (periodicStats.state) "з інтернетом" else "без інтернету"
        }
        sb.append("Найдовший період $byType: ${periodicStats.longestPeriod.humanReadable()}\n")
        sb.append("Найкоротший період $byType: ${periodicStats.shortestPeriod.humanReadable()}\n")
        sb.append("Медіанний період $byType: ${periodicStats.medianPeriod.humanReadable()}\n\n")
    }
}
