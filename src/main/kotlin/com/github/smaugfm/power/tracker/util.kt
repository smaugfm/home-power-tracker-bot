package com.github.smaugfm.power.tracker

fun Int.isZero() = this == 0

fun Double.format(digits: Int = 2) = "%.${digits}f".format(this)

fun List<Long>.median(): Double {
    val sorted = this.sorted()

    return if (sorted.size % 2 == 0) {
        ((sorted[sorted.size / 2] + sorted[sorted.size / 2 - 1]) / 2).toDouble()
    } else {
        (sorted[sorted.size / 2]).toDouble()
    }
}
