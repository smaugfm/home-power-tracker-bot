package com.github.smaugfm.power.tracker.interaction.telegram.handlers

import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.from
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.MessageContent
import dev.inmo.tgbotapi.utils.RiskFeature
import kotlinx.coroutines.channels.Channel
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Profile("!test")
@OptIn(RiskFeature::class)
@Component
class DeleteEventHandler(
    @Qualifier("replyMessagesChannel")
    private val replyMessagesChannel: Channel<CommonMessage<MessageContent>>,
) {

    suspend fun handle(
        context: BehaviourContext,
        message: CommonMessage<MessageContent>
    ): Boolean = context.internal(message)

    private suspend fun BehaviourContext.internal(
        message: CommonMessage<MessageContent>
    ): Boolean {
        val replyTo = message.replyTo
        if (replyTo != null && replyTo.from?.id == bot.getMe().id) {
            log.info { "Received Telegram message forwarded from bot: $message" }
            replyMessagesChannel.send(message)
            message.from?.let {
                bot.deleteMessage(it, message.messageId)
            }
            return true
        }
        return false
    }
}
