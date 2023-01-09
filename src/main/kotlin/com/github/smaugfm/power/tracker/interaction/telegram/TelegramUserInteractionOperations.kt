package com.github.smaugfm.power.tracker.interaction.telegram

import com.github.smaugfm.power.tracker.ConfigId
import com.github.smaugfm.power.tracker.Event
import com.github.smaugfm.power.tracker.EventId
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingleOrNull
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.time.Duration

private val log = KotlinLogging.logger { }

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
    private val exportCommandMessagesChannel: ReceiveChannel<CommonMessage<MessageContent>>
) : UserInteractionOperations {
    override suspend fun postForEvent(event: Event, stats: List<EventStats>) {
        val text = formatter.getTelegramMessage(stats)

        chatIdRepository
            .findAllByConfigId(event.configId)
            .asFlow()
            .map {
                try {
                    val msg = bot.sendTextMessage(
                        ChatId(it.chatId),
                        TelegramMarkdownV2Format.escape(text),
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
                        TelegramMarkdownV2Format.escape(newText),
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

    override suspend fun postExport(configId: ConfigId, events: Flow<Event>) {
        log.warn { "TODO: EXPORT NOT IMPLEMENTED" }
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
    override fun deletionFlow(): Flow<EventId> =
        replyMessagesChannel
            .consumeAsFlow()
            .mapNotNull { message ->
                message.replyTo?.messageId?.let {
                    messagesRepository
                        .findByMessageIdAndChatId(it, message.chat.id.chatId)
                        .awaitSingleOrNull()
                }.also {
                    if (it == null)
                        log.warn {
                            "User chatId=${message.chat.id.chatId} replied to " +
                                    "messageId=${message.replyTo?.messageId} without an attached event."
                        }
                }
            }.map {
                it.eventId
            }

    @RiskFeature
    override fun exportFlow(): Flow<ConfigId> =
        exportCommandMessagesChannel
            .consumeAsFlow()
            .mapNotNull {
                val id = it.from?.id?.chatId ?: return@mapNotNull null

                chatIdRepository.findById(id).awaitSingleOrNull()?.configId
            }
}
