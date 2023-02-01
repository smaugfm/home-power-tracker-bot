package com.github.smaugfm.power.tracker.interaction.telegram.handlers

import com.github.smaugfm.power.tracker.NewConfig
import com.github.smaugfm.power.tracker.YasnoGroup
import com.github.smaugfm.power.tracker.config.ConfigService
import com.github.smaugfm.power.tracker.interaction.telegram.TelegramUserInteractionOperations
import com.github.smaugfm.power.tracker.network.PingService
import com.github.smaugfm.power.tracker.waitTextRegex
import com.github.smaugfm.power.tracker.yesNoToBoolean
import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.types.buttons.ReplyKeyboardMarkup
import dev.inmo.tgbotapi.requests.send.SendTextMessage
import dev.inmo.tgbotapi.types.IdChatIdentifier
import dev.inmo.tgbotapi.types.buttons.ReplyKeyboardRemove
import dev.inmo.tgbotapi.types.buttons.SimpleKeyboardButton
import dev.inmo.tgbotapi.utils.EntitiesBuilder
import dev.inmo.tgbotapi.utils.bold
import dev.inmo.tgbotapi.utils.link
import dev.inmo.tgbotapi.utils.regular
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
        context.internal(scope, replyToChatId)
    }

    private suspend fun BehaviourContext.internal(
        scope: CoroutineScope,
        replyToChatId: IdChatIdentifier
    ) {
        if (checkTelegramChatIdAlreadyConfigured(replyToChatId)) return

        val address = promptUserForAddress(replyToChatId) ?: return

        if (checkExistingConfig(address, replyToChatId)) return

        addNewConfig(replyToChatId, scope, address)
    }

    private suspend fun BehaviourContext.checkTelegramChatIdAlreadyConfigured(replyToChatId: IdChatIdentifier): Boolean {
        val existingConfigId =
            telegramUserInteractionOperations.getConfigId(replyToChatId.chatId)
        if (existingConfigId != null) {
            val config = configService.getById(existingConfigId)
            if (config != null) {
                val answer = waitTextRegex(
                    SendTextMessage(
                        replyToChatId,
                        EntitiesBuilder()
                            .regular("У тебе вже сконфігуровано отримання сповіщень для адреси роутера ")
                            .bold(config.address)
                            .regular(". Чи бажаєш змінити адресу?")
                            .build(),
                        replyMarkup = ReplyKeyboardMarkup(
                            SimpleKeyboardButton("Так"),
                            SimpleKeyboardButton("Ні"),
                            resizeKeyboard = true,
                            oneTimeKeyboard = true,
                        ),
                    ),
                    Regex("^Так|Ні$"),
                    {
                        sendTextMessage(replyToChatId, "Це не схоже на відповідь Так або Ні.")
                    },
                )?.yesNoToBoolean() ?: return true

                if (!answer) {
                    sendTextMessage(
                        replyToChatId,
                        "Добре, залишаю стару адресу"
                    )
                    return true
                }
            } else {
                log.warn {
                    "Found dangling TelegramChatIdEntity. " +
                            "chatId=${replyToChatId.chatId}, configId=${existingConfigId}"
                }
            }
        }
        return false
    }

    private suspend fun BehaviourContext.promptUserForAddress(replyToChatId: IdChatIdentifier): String? =
        waitTextRegex(
            SendTextMessage(replyToChatId, "Введи ip-адресу або hostname твого роутера:"),
            listOf(
                Regex("""^((25[0-5]|(2[0-4]|1\d|[1-9]|)\d)\.?\b){4}$"""),
                Regex("""^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\-]*[a-zA-Z0-9])\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\-]*[A-Za-z0-9])$"""),
            ),
            {
                sendTextMessage(replyToChatId, "Це не схоже на ip-адресу чи hostname. Спробуй ще раз або /exit")
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
                replyToChatId,
                "Не бачу твій роутер. Можливо в тебе зараз нема світла, " +
                        "або не правильно вказана ip-адреса чи hostname. Також в тебе має бути відкритим " +
                        "TCP порт 1 і роутер має приймати на на нього ICMP (ping) пакети.\n" +
                        "В будь-якому разі, можеш спробувати ще раз /start"
            )
            return
        }

        val group = waitTextRegex(
            SendTextMessage(
                replyToChatId,
                EntitiesBuilder()
                    .regular("Яка в тебе група відключень? Це можна подивитись ")
                    .link("тут", "https://kyiv.yasno.com.ua/schedule-turn-off-electricity")
                    .build(),
                replyMarkup = ReplyKeyboardMarkup(
                    SimpleKeyboardButton("1"),
                    SimpleKeyboardButton("2"),
                    resizeKeyboard = true,
                    oneTimeKeyboard = true,
                )
            ),
            Regex("^1|2$"),
            {
                sendTextMessage(
                    replyToChatId,
                    "Це не схоже на номер групи. Будь ласка спробуй ще раз або відправ /exit"
                )
            }) ?: return
        val yasnoGroup = YasnoGroup.fromString(group)
        val newConfig = NewConfig(address, yasnoGroup, null)

        val config = configService.addNewConfig(newConfig)
        telegramUserInteractionOperations.addNewChatId(
            replyToChatId.chatId,
            config.id
        )

        sendTextMessage(
            replyToChatId,
            "Все, тепер ти отримуватимеш сповіщення коли в тебе дома з'являється чи пропадає світло",
            replyMarkup = ReplyKeyboardRemove()
        )
    }
}
