package com.github.smaugfm.power.tracker

import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitDataCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitText
import dev.inmo.tgbotapi.extensions.utils.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.requests.abstracts.Request
import dev.inmo.tgbotapi.requests.send.SendTextMessage
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.CallbackDataInlineKeyboardButton
import kotlinx.coroutines.flow.firstOrNull
import net.time4j.PrettyTime
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.*

private val minute = Duration.ofMinutes(1)

fun Int.isZero() = this == 0

fun Double.format(digits: Int = 2) = "%.${digits}f".format(this)

fun Collection<NewEvent>.separateInitial(): Pair<List<NewEvent.Initial>, List<NewEvent.Common>> {
    val initial: MutableList<NewEvent.Initial> = mutableListOf()
    val common: MutableList<NewEvent.Common> = mutableListOf()
    this.forEach {
        when (it) {
            is NewEvent.Common -> common.add(it)
            is NewEvent.Initial -> initial.add(it)
        }
    }
    return Pair(initial, common)
}

fun Collection<Long>.median(): Double {
    val sorted = this.sorted()

    return if (sorted.size % 2 == 0) {
        ((sorted[sorted.size / 2] + sorted[sorted.size / 2 - 1]) / 2).toDouble()
    } else {
        (sorted[sorted.size / 2]).toDouble()
    }
}

inline fun <T : Any> T?.ifNull(action: () -> Unit) = this.also {
    if (it == null) action()
}

fun getResourceAsText(path: String): String? = object {}.javaClass.classLoader.getResource(path)?.readText()

fun Duration.humanReadable(): String {
    val truncated = this.truncatedTo(ChronoUnit.MINUTES)
    return if (truncated < minute) "меньше хвилини"
    else PrettyTime.of(Locale("uk", "UA")).print(truncated)

}

suspend fun BehaviourContext.waitYesNo(initRequest: SendTextMessage): Boolean? {
    val answer = waitMenuButtons(initRequest, "так", "ні") ?: return null

    return when (answer.lowercase()) {
        "так" -> true
        "ні" -> false
        else -> null
    }
}

suspend fun BehaviourContext.waitMenuButtons(
    initRequest: SendTextMessage,
    vararg buttons: String,
): String? {
    assert(buttons.distinct() == buttons.toList())

    return waitDataCallbackQuery(
        initRequest.copy(
            replyMarkup = InlineKeyboardMarkup(
                *buttons.map { CallbackDataInlineKeyboardButton(it, it) }.toTypedArray()
            )
        )
    ).firstOrNull()?.data
}

suspend fun BehaviourContext.waitTextRegex(
    initRequest: Request<*>? = null,
    checks: List<Regex>,
    illegalHandler: suspend BehaviourContext.() -> Unit,
    exitCommand: String = "exit",
): String? {
    var text: String?
    do {
        text = waitText(initRequest) { null }.firstOrNull()?.text
        if (text == "/$exitCommand")
            return null
        if (text == null || checks.all { !it.matches(text) })
            illegalHandler()
        else
            break
    } while (true)
    return text
}

suspend fun BehaviourContext.waitTextRegex(
    initRequest: Request<*>? = null,
    check: Regex,
    illegalHandler: suspend BehaviourContext.() -> Unit,
    exitCommand: String = "exit",
): String? = waitTextRegex(initRequest, listOf(check), illegalHandler, exitCommand)
