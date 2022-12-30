package com.github.smaugfm.power.tracker.spring

import kotlinx.coroutines.CoroutineScope

interface LaunchCoroutineBean {
    suspend fun launch(scope: CoroutineScope)
}
