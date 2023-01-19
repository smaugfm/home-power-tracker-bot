package com.github.smaugfm.power.tracker

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
