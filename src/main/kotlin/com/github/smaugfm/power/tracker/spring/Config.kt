package com.github.smaugfm.power.tracker.spring

import com.github.smaugfm.power.tracker.events.EventsService
import com.github.smaugfm.power.tracker.interaction.UserInteractionService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.auditing.DateTimeProvider
import java.time.ZonedDateTime
import java.util.Optional

@Configuration
class Config {
    @Bean
    fun zonedDateTimeProvider(): DateTimeProvider =
        DateTimeProvider { Optional.of(ZonedDateTime.now()) }
}
