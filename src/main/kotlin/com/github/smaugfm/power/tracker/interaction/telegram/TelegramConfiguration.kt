package com.github.smaugfm.power.tracker.interaction.telegram

import dev.inmo.tgbotapi.bot.ktor.telegramBot
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.MessageContent
import kotlinx.coroutines.channels.Channel
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Profile("!test")
@Configuration
class TelegramConfiguration {

    @Bean
    fun replyMessagesChannel(): Channel<CommonMessage<MessageContent>> = Channel()

    @Bean
    fun exportCommandMessagesChannel(): Channel<CommonMessage<MessageContent>> = Channel()

    @Bean
    fun statsCommandMessagesChannel(): Channel<CommonMessage<MessageContent>> = Channel()


    @Bean
    fun bot(@Value("\${bot.token}") token: String) =
        telegramBot(token)
}
