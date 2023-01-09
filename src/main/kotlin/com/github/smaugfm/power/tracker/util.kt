package com.github.smaugfm.power.tracker

fun Double.format(digits: Int = 2) = "%.${digits}f".format(this)
