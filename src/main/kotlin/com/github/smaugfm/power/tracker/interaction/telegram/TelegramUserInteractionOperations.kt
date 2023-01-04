package com.github.smaugfm.power.tracker.interaction.telegram

import com.github.smaugfm.power.tracker.dto.ConfigId
import com.github.smaugfm.power.tracker.dto.Event
import com.github.smaugfm.power.tracker.dto.EventId
import com.github.smaugfm.power.tracker.interaction.UserInteractionOperations
import com.github.smaugfm.power.tracker.persistence.TelegramChatIdsRepository
import com.github.smaugfm.power.tracker.persistence.TelegramMessageEntity
import com.github.smaugfm.power.tracker.persistence.TelegramMessagesRepository
import com.github.smaugfm.power.tracker.stats.EventStats
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.message.MarkdownV2ParseMode
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingleOrNull
import mu.KotlinLogging
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
    private val formatter: TelegramMessageCreator
) : UserInteractionOperations {
    override suspend fun postEvent(event: Event, stats: List<EventStats>) {
        val texts = formatter.getTelegramMessages(stats).asFlow()

        chatIdRepository
            .findAllByConfigId(event.configId)
            .asFlow()
            .flatMapMerge {
                texts.map { text ->
                    try {
                        val msg = bot.sendTextMessage(ChatId(it.chatId), text, MarkdownV2ParseMode)
                        messagesRepository.save(
                            TelegramMessageEntity(
                                msg.messageId,
                                it.chatId,
                                event.id
                            )
                        ).awaitSingleOrNull()
                    } catch (e: Throwable) {
                        log.error(e) { "Error posting event to Telegram chatId=${it.chatId}" }
                        null
                    }
                }
            }.toList()
            .also {
                log.info {
                    "Posted Telegram messages: $it"
                }
            }
    }

    override suspend fun updateEvent(event: Event, stats: List<EventStats>) {
        TODO("Not yet implemented")
    }

    override suspend fun postExport(configId: ConfigId, events: Flow<Event>) {
        TODO("Not yet implemented")
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

    override fun deletionFlow(): Flow<EventId> {
        //TODO
        return emptyFlow()
    }

    override fun exportFlow(): Flow<ConfigId> {
        //TODO
        return emptyFlow()
    }
}
