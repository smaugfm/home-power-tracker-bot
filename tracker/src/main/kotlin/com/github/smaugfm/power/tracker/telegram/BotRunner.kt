package com.github.smaugfm.power.tracker.telegram

import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.types.message.textsources.regular
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class BotRunner(
    @Value("\${bot.token}") private val token: String,
) : CommandLineRunner {
    override fun run(vararg args: String?) {
        runBlocking {
            val (bot, job) = telegramBotWithBehaviourAndLongPolling(
                token,
                CoroutineScope(Dispatchers.IO)
            ) {
                onCommand("start") {
                    sendTextMessage(it.chat, "Hi:)")
                }
            }
            log.info(bot.getMe().toString())
            job.join()
        }
    }
}