package com.github.smaugfm.power.tracker.interaction.telegram.handlers

import com.github.smaugfm.power.tracker.NewConfig
import com.github.smaugfm.power.tracker.YasnoGroup
import com.github.smaugfm.power.tracker.config.ConfigService
import com.github.smaugfm.power.tracker.interaction.telegram.TelegramUserInteractionOperations
import com.github.smaugfm.power.tracker.network.PingService
import com.github.smaugfm.power.tracker.waitMenuButtons
import com.github.smaugfm.power.tracker.waitTextRegex
import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.requests.send.SendTextMessage
import dev.inmo.tgbotapi.types.IdChatIdentifier
import dev.inmo.tgbotapi.utils.*
import kotlinx.coroutines.CoroutineScope
import mu.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class StartCommandHandler(
    private val ping: PingService,
    private val telegramUserInteractionOperations: TelegramUserInteractionOperations,
    private val configService: ConfigService,
) {

    suspend fun handle(
        context: BehaviourContext,
        scope: CoroutineScope,
        replyToChatId: IdChatIdentifier
    ) {
        try {
            context.internal(scope, replyToChatId)
        } catch (e: Throwable) {
            log.error(e) { "unexpected error" }
            context.sendTextMessage(
                replyToChatId,
                "Сталася внутрішня помилка. Спробуй ще раз /start",
            )
        }
    }

    private suspend fun BehaviourContext.internal(
        scope: CoroutineScope,
        replyToChatId: IdChatIdentifier
    ) {
        val existingConfigIdByChatId =
            telegramUserInteractionOperations.getConfigIdByChatId(replyToChatId.chatId)
        if (existingConfigIdByChatId != null) {
            errorTelegramChatIdAlreadyConfigured(replyToChatId, existingConfigIdByChatId)
            return
        }

        val address = promptUserForAddress(replyToChatId) ?: return

        if (checkExistingConfig(address, replyToChatId))
            return

        addNewConfig(replyToChatId, scope, address)
    }

    private suspend fun BehaviourContext.errorTelegramChatIdAlreadyConfigured(
        replyToChatId: IdChatIdentifier,
        existingConfigId: Long,
    ) {
        val config = configService.getById(existingConfigId)
        if (config != null) {
            sendTextMessage(replyToChatId) {
                regular("У тебе вже сконфігуровано отримання сповіщень для адреси роутера ")
                bold(config.address)
                regular(
                    ". Наразі зміна адреси не підтримується.\n" +
                            "Якщо тобі вкрай потрібно це зробити, будь-ласка напиши розробнику бота "
                )
                mention("smaugfm")
            }
        } else {
            log.warn {
                "Found dangling TelegramChatIdEntity. " +
                        "chatId=${replyToChatId.chatId}, configId=${existingConfigId}"
            }
        }
    }

    private suspend fun BehaviourContext.promptUserForAddress(replyToChatId: IdChatIdentifier): String? =
        waitTextRegex(
            SendTextMessage(
                replyToChatId,
                "Введи IP-адресу або DNS ім'я твого роутера:",
            ),
            listOf(
                Regex("""^((25[0-5]|(2[0-4]|1\d|[1-9]|)\d)\.?\b){4}$"""),
                Regex("""^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\-]*[a-zA-Z0-9])\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\-]*[A-Za-z0-9])$"""),
            ),
            {
                sendTextMessage(replyToChatId, "Це не схоже на ip-адресу чи DNS ім'я. Спробуй ще раз або /exit")
            })

    private suspend fun BehaviourContext.checkExistingConfig(
        address: String,
        replyToChatId: IdChatIdentifier
    ): Boolean {
        val existingConfig = configService.getByAddress(address)
        if (existingConfig != null) {
            telegramUserInteractionOperations.addNewChatId(
                replyToChatId.chatId, existingConfig.id
            )
            sendTextMessage(
                replyToChatId,
                "Все, тепер ти отримуватимеш сповіщення коли в тебе дома з'являється чи пропадає світло"
            )
            return true
        }
        return false
    }

    private suspend fun BehaviourContext.addNewConfig(
        replyToChatId: IdChatIdentifier,
        scope: CoroutineScope,
        address: String
    ) {
        sendTextMessage(
            replyToChatId,
            "Перевіряю чи пінгується твій роутер..."
        )

        val state = ping.ping(scope, address, null)

        if (state.hasPower != true) {
            sendTextMessage(
                replyToChatId
            ) {
                regular(
                    "Не бачу твій роутер.\nМожливо в тебе зараз нема світла, " +
                            "або не правильно вказана ip-адреса чи DNS ім'я роутера.\n" +
                            "Також у тебе має бути відкритим "
                )
                bold("TCP порт 1")
                regular(" і роутер має приймати на на нього ")
                bold("ICMP")
                regular(" пакети (від команди ping).\n\n")
                regular("В будь-якому разі, можеш спробувати /start пізніше")
            }
            return
        }

        val group = waitMenuButtons(
            SendTextMessage(
                replyToChatId,
                EntitiesBuilder()
                    .regular(
                        "Яка в тебе група відключень для міста Києва? " +
                                "Це можна подивитись на сайті "
                    )
                    .link("kyiv.yasno.com", "https://kyiv.yasno.com.ua/schedule-turn-off-electricity")
                    .build(),
            ),
            "1", "2"
        )
        val yasnoGroup = YasnoGroup.fromString(group!!)
        val newConfig = NewConfig(address, yasnoGroup, null)

        val config = configService.addNewConfig(newConfig)
        telegramUserInteractionOperations.addNewChatId(
            replyToChatId.chatId,
            config.id
        )

        sendTextMessage(
            replyToChatId,
            "Все, тепер ти отримуватимеш сповіщення коли в тебе дома з'являється чи пропадає світло",
        )
    }
}
