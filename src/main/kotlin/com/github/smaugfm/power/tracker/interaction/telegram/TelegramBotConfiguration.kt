package com.github.smaugfm.power.tracker.interaction.telegram

import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

private val log = KotlinLogging.logger {}

@Profile("!test")
@Configuration
class TelegramBotConfiguration {

    @Bean
    fun startBot(
        scope: CoroutineScope,
        @Value("\${bot.token}") token: String,
    ) = scope.launch {
        val (bot, job) = telegramBotWithBehaviourAndLongPolling(
            token, CoroutineScope(Dispatchers.IO)
        ) {
            onCommand("start") {
                sendTextMessage(it.chat, "Hi:)")
            }
        }
        val botStr = bot.getMe().toString()
        log.info { "Telegram bot started: $botStr" }
    }
}
