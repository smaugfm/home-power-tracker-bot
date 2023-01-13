package com.github.smaugfm.power.tracker.interaction.telegram

import com.github.smaugfm.power.tracker.*
import com.github.smaugfm.power.tracker.UserInteractionData.TelegramUserInteractionData
import com.github.smaugfm.power.tracker.interaction.UserInteractionOperations
import com.github.smaugfm.power.tracker.persistence.TelegramChatIdsRepository
import com.github.smaugfm.power.tracker.persistence.TelegramMessageEntity
import com.github.smaugfm.power.tracker.persistence.TelegramMessagesRepository
import com.github.smaugfm.power.tracker.stats.EventStats
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.edit.text.editMessageText
import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.from
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.message.MarkdownV2ParseMode
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.MessageContent
import dev.inmo.tgbotapi.utils.RiskFeature
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingleOrNull
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.time.Duration

private val log = KotlinLogging.logger { }

@RiskFeature
@Profile("!test")
@Component
@FlowPreview
class TelegramUserInteractionOperations(
    private val bot: TelegramBot,
    private val chatIdRepository: TelegramChatIdsRepository,
    private val messagesRepository: TelegramMessagesRepository,
    private val formatter: TelegramMessageCreator,
    @Qualifier("replyMessagesChannel")
    private val replyMessagesChannel: ReceiveChannel<CommonMessage<MessageContent>>,
    @Qualifier("exportCommandMessagesChannel")
    private val exportCommandMessagesChannel: ReceiveChannel<CommonMessage<MessageContent>>,
    @Qualifier("statsCommandMessagesChannel")
    private val statsCommandMessagesChannel: ReceiveChannel<CommonMessage<MessageContent>>,
) : UserInteractionOperations<TelegramUserInteractionData> {
    override suspend fun postForEvent(event: Event, stats: List<EventStats>) {
        val text = formatter.getTelegramMessage(stats)

        chatIdRepository
            .findAllByConfigId(event.configId)
            .asFlow()
            .map {
                try {
                    val msg = bot.sendTextMessage(
                        ChatId(it.chatId),
                        text,
                        MarkdownV2ParseMode
                    )
                    messagesRepository.save(
                        TelegramMessageEntity(
                            msg.messageId,
                            it.chatId,
                            event.id
                        )
                    ).awaitSingleOrNull()
                } catch (e: Throwable) {
                    log.error(e) { "Error posting event $event to Telegram chatId=${it.chatId}" }
                    null
                }
            }.toList()
            .also {
                log.info {
                    "Posted Telegram messages: $it"
                }
            }
    }

    override suspend fun updateForEvent(event: Event, stats: List<EventStats>) {
        val newText = formatter.getTelegramMessage(stats)
        messagesRepository
            .findAllByEventId(event.id)
            .switchIfEmpty {
                log.warn { "Did not find any messages to update for event $event" }
            }
            .asFlow()
            .map { messageEntity ->
                try {
                    bot.editMessageText(
                        ChatId(messageEntity.chatId),
                        messageEntity.messageId,
                        newText,
                        MarkdownV2ParseMode
                    )
                    log.info {
                        "Updated Telegram messageId=${messageEntity.messageId} in " +
                                "chatId=${messageEntity.chatId} for event $event with new stats"
                    }
                } catch (e: Throwable) {
                    log.error(e) {
                        "Error updating Telegram messageId=${messageEntity.messageId} " +
                                "in chatId=${messageEntity.chatId} for event: $event"
                    }
                }
            }
            .collect()
    }

    override suspend fun deleteForEvent(event: Event) {
        messagesRepository
            .findAllByEventId(event.id)
            .switchIfEmpty {
                log.warn { "Did not find message to delete for event $event" }
            }
            .asFlow()
            .map { messageEntity ->
                try {
                    messagesRepository
                        .deleteById(messageEntity.id)
                        .awaitSingleOrNull()
                    bot.deleteMessage(ChatId(messageEntity.chatId), messageEntity.messageId)
                } catch (e: Throwable) {
                    log.error(e) {
                        "Error deleting Telegram messageId=${messageEntity.id} " +
                                "in chatId=${messageEntity.chatId} for event: $event}"
                    }
                }
            }
            .collect()
    }

    override suspend fun postExport(data: TelegramUserInteractionData, events: Flow<Event>) {
        log.warn { "TODO: EXPORT NOT IMPLEMENTED" }
    }

    override suspend fun postStats(data: TelegramUserInteractionData, stats: EventStats.Summary) {
        val text = formatter.getTelegramMessage(listOf(stats))
        bot.sendTextMessage(ChatId(data.telegramChatId), text, MarkdownV2ParseMode)
    }

    override suspend fun postUnstableNetworkTimeout(duration: Duration) {
        val text = formatter.unstableNetworkMessage(duration)
        chatIdRepository
            .findAll()
            .asFlow()
            .map {
                bot.sendTextMessage(ChatId(it.chatId), text)
            }.toList()
            .also {
                log.info {
                    "Posted unstable network Telegram messages: " +
                            "${it.map { "<messageId=${it.messageId}, chatId=${it.chat.id.chatId}>" }}"
                }
            }
    }

    @RiskFeature
    override fun deletionFlow(): Flow<EventDeletionRequest<TelegramUserInteractionData>> =
        replyMessagesChannel
            .consumeAsFlow()
            .mapNotNull { message ->
                val chatId = getChatIdFromReply(message) ?: return@mapNotNull null
                val configId = getConfigId(chatId, message.messageId) ?: return@mapNotNull null
                val messageEntity = message.replyTo?.messageId?.let {
                    messagesRepository
                        .findByMessageIdAndChatId(it, message.chat.id.chatId)
                        .awaitSingleOrNull()
                }.ifNull {
                    log.warn {
                        "User chatId=${message.chat.id.chatId} replied to " +
                                "messageId=${message.replyTo?.messageId} without an attached event."
                    }
                } ?: return@mapNotNull null

                EventDeletionRequest(
                    TelegramUserInteractionData(
                        configId,
                        message.messageId,
                        chatId
                    ),
                    messageEntity.eventId
                )
            }

    override fun exportFlow() =
        messagesToUserInteractionData(exportCommandMessagesChannel)

    override fun statsFlow() =
        messagesToUserInteractionData(statsCommandMessagesChannel)

    private fun messagesToUserInteractionData(
        channel: ReceiveChannel<CommonMessage<MessageContent>>
    ): Flow<TelegramUserInteractionData> =
        channel
            .consumeAsFlow()
            .mapNotNull { message ->
                val chatId = getChatId(message) ?: return@mapNotNull null
                val configId = getConfigId(chatId, message.messageId) ?: return@mapNotNull null

                TelegramUserInteractionData(
                    configId,
                    message.messageId,
                    chatId
                )
            }

    private suspend fun getChatId(message: CommonMessage<MessageContent>): Long? =
        message.from?.id?.chatId.ifNull {
            log.warn { "Not chatId in messageId=${message.messageId}" }
        }

    private suspend fun getChatIdFromReply(message: CommonMessage<MessageContent>): Long? =
        message.replyTo?.chat?.id?.chatId.ifNull {
            log.warn { "Not chatId in messageId=${message.messageId}" }
        }

    private suspend fun getConfigId(chatId: Long, messageId: Long): Long? =
        chatIdRepository.findById(chatId).awaitSingleOrNull()?.configId.ifNull {
            log.warn {
                "User chatId=${chatId} sent messageId=${messageId}" +
                        "but not chatId was found in the DB"
            }
        }
}
