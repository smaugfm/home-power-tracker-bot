package com.github.smaugfm.power.tracker.interaction.telegram

import com.github.smaugfm.power.tracker.interaction.telegram.handlers.DeleteEventHandler
import com.github.smaugfm.power.tracker.interaction.telegram.handlers.StartCommandHandler
import com.github.smaugfm.power.tracker.spring.LaunchCoroutineBean
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onContentMessage
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.MessageContent
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.utils.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Profile("!test")
@Component
class TelegramBotLauncher(
    private val start: StartCommandHandler,
    private val delete: DeleteEventHandler,
    private val bot: TelegramBot,
    @Qualifier("exportCommandMessagesChannel")
    private val exportCommandMessagesChannel: Channel<CommonMessage<MessageContent>>,
    @Qualifier("statsCommandMessagesChannel")
    private val statsCommandMessagesChannel: Channel<CommonMessage<MessageContent>>,
) : LaunchCoroutineBean {

    override suspend fun launch(scope: CoroutineScope) {
        val job = bot.buildBehaviourWithLongPolling(scope = scope) {
            onCommand("start") {
                logCommand(it, "start")
                start.handle(this, scope, it.chat.id)
            }
            onCommand("stats") {
                logCommand(it, "stats")
                statsCommandMessagesChannel.send(it)
            }
            onCommand("export") {
                logCommand(it, "export")
                exportCommandMessagesChannel.send(it)
            }
            onContentMessage { message ->
                if (!delete.handle(this, message))
                    log.info { "Received Telegram message: $message" }
            }
        }
        val botStr = bot.getMe().toString()
        log.info { "Telegram bot started: $botStr" }
        job.join()
    }

    private fun logCommand(it: CommonMessage<TextContent>, cmd: String) {
        log.info("Received Telegram /$cmd command: $it")
    }
}
