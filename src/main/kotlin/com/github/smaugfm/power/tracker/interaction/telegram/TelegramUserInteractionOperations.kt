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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("!test")
@Component
class TelegramUserInteractionOperations(
    private val bot: TelegramBot,
    private val chatIdRepository: TelegramChatIdsRepository,
    private val messagesRepository: TelegramMessagesRepository,
    private val formatter: TelegramMessageFormatter
) : UserInteractionOperations {
    override suspend fun postEvent(event: Event, stats: EventStats) {
        val text = formatter.formatMessage(event, stats)

        chatIdRepository.findAllByConfigId(event.configId).asFlow().collect {
            val msg = bot.sendTextMessage(ChatId(it.chatId), text, MarkdownV2ParseMode)

            messagesRepository.save(
                TelegramMessageEntity(
                    msg.messageId,
                    it.chatId,
                    it.configId
                )
            ).awaitSingleOrNull()
        }
    }

    override suspend fun updateEvent(event: Event, stat: EventStats) {
        TODO("Not yet implemented")
    }

    override suspend fun postExport(configId: ConfigId, events: Flow<Event>) {
        TODO("Not yet implemented")
    }

    override fun deletionFlow(): Flow<EventId> {
        TODO("Not yet implemented")
    }

    override fun exportFlow(): Flow<ConfigId> {
        TODO("Not yet implemented")
    }
}
