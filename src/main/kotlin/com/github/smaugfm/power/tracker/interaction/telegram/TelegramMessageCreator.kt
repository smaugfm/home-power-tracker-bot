package com.github.smaugfm.power.tracker.interaction.telegram

import com.github.smaugfm.power.tracker.dto.EventType
import com.github.smaugfm.power.tracker.stats.EventStats
import net.time4j.PrettyTime
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.Locale

@Component
class TelegramMessageCreator {
    fun getTelegramMessages(stats: List<EventStats>): List<String> =
        stats.map(::createText)

    fun unstableNetworkMessage(duration: Duration) =
        "Мережа на сервері бота нестабільна.\nЧас поганої роботи мережі: ${humanReadable(duration)}"

    private fun createText(stats: EventStats): String =
        when (stats) {
            is EventStats.Single ->
                "${simpleSingleEventText(stats)} \n\n ${extendedSingleEventText(stats)}"

            is EventStats.Summary -> TODO()
        }

    private fun extendedSingleEventText(stats: EventStats.Single) =
        when (stats) {
            is EventStats.Single.LastInverseOnly -> {
                if (stats.state)
                    "Скільки не було: " + this.humanReadable(stats.lastInverse)
                else
                    when (stats.type) {
                        EventType.POWER -> "Скільки трималось: "
                        EventType.ISP -> "Скільки тримався: "
                    } + this.humanReadable(stats.lastInverse)
            }

            is EventStats.Single.IspDownStats -> {
                val str =
                    StringBuilder("Скільки був: ${this.humanReadable(stats.lastInverse)}.")
                if (stats.lastUPSOperation != null)
                    str.append("\nЗ цього, працював ДБЖ: ${this.humanReadable(stats.lastUPSOperation)}")
                if (stats.lastUPSCharge != null)
                    str.append(
                        "\nТривалість останньої зарядки акумуляторів ДБЖ: ${
                            this.humanReadable(
                                stats.lastUPSCharge
                            )
                        }"
                    )
                str.toString()
            }
        }

    private fun simpleSingleEventText(stats: EventStats.Single) =
        when (stats.type) {
            EventType.POWER -> "${if (stats.state) "🟢" else "🔴"} Світло " +
                    if (stats.state) "відновлено" else "зникло"

            EventType.ISP -> "${if (stats.state) "🟩" else "🟥"} Інтернет " +
                    if (stats.state) "з'явився" else "зник"
        }

    private fun humanReadable(duration: Duration): String =
        PrettyTime.of(Locale("uk", "UA"))
            .print(duration.truncatedTo(ChronoUnit.MINUTES))
}
