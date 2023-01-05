package com.github.smaugfm.power.tracker.util

fun Int.isZero() = this == 0

fun Double.format(digits: Int = 2) = "%.${digits}f".format(this)
