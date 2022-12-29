package com.github.smaugfm.power.tracker.interaction.telegram

import com.github.smaugfm.power.tracker.spring.CoroutinesLaunchAdapter
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.bot.ktor.telegramBot
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

private val log = KotlinLogging.logger {}

@Profile("!test")
@Configuration
class TelegramBotConfiguration {

    @Bean
    fun bot(@Value("\${bot.token}") token: String): TelegramBot {
        return telegramBot(token)
    }

    @Bean
    fun startBotJob(
        context: ApplicationContext,
        bot: TelegramBot,
    ): CoroutinesLaunchAdapter {
        return object : CoroutinesLaunchAdapter {
            override suspend fun launch() {
                val job = bot.buildBehaviourWithLongPolling {

                }
                val botStr = bot.getMe().toString()
                log.info { "Telegram bot started: $botStr" }
                job.join()
            }
        }
    }
}
