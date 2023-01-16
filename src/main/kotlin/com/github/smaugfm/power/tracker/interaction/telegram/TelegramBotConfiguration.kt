package com.github.smaugfm.power.tracker.interaction.telegram

import com.github.smaugfm.power.tracker.spring.LaunchCoroutineBean
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.bot.ktor.telegramBot
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommandWithArgs
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onContentMessage
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.from
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.MessageContent
import dev.inmo.tgbotapi.utils.RiskFeature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
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
    fun replyMessagesChannel(): Channel<CommonMessage<MessageContent>> = Channel()

    @Bean
    fun exportCommandMessagesChannel(): Channel<CommonMessage<MessageContent>> = Channel()

    @Bean
    fun statsCommandMessagesChannel(): Channel<CommonMessage<MessageContent>> = Channel()

    @RiskFeature
    @Bean
    fun startBotJob(
        bot: TelegramBot,
        @Qualifier("replyMessagesChannel")
        replyMessagesChannel: Channel<CommonMessage<MessageContent>>,
        @Qualifier("exportCommandMessagesChannel")
        exportCommandMessagesChannel: Channel<CommonMessage<MessageContent>>,
        @Qualifier("statsCommandMessagesChannel")
        statsCommandMessagesChannel: Channel<CommonMessage<MessageContent>>,
    ): LaunchCoroutineBean =
        object : LaunchCoroutineBean {
            override suspend fun launch(scope: CoroutineScope) {
                val job = bot.buildBehaviourWithLongPolling(scope = scope) {
                    onCommand("stats") {
                        log.info { "Received Telegram /stats command: $it" }
                        statsCommandMessagesChannel.send(it)
                    }
                    onCommand("export") {
                        log.info { "Received Telegram /export command: $it" }
                        exportCommandMessagesChannel.send(it)
                    }
                    onContentMessage { message ->
                        val replyTo = message.replyTo
                        if (replyTo != null && replyTo.from?.id == bot.getMe().id) {
                            log.info { "Received Telegram message forwarded from bot: $message" }
                            replyMessagesChannel.send(message)
                            message.from?.let {
                                bot.deleteMessage(it, message.messageId)
                            }
                        } else
                            log.info { "Received Telegram message: $message" }
                    }
                }
                val botStr = bot.getMe().toString()
                log.info { "Telegram bot started: $botStr" }
                job.join()
            }
        }
}
