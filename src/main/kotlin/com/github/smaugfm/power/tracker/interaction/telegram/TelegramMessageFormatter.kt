package com.github.smaugfm.power.tracker.interaction.telegram

import com.github.smaugfm.power.tracker.dto.Event
import com.github.smaugfm.power.tracker.stats.EventStats
import org.springframework.stereotype.Component

@Component
class TelegramMessageFormatter {
    fun formatMessage(event: Event, stats: EventStats): String {
        TODO("Not yet implemented")
    }
}
