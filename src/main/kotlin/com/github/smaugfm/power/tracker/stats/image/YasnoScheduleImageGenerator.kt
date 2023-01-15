package com.github.smaugfm.power.tracker.stats.image

import com.github.smaugfm.power.tracker.YasnoGroup

interface YasnoScheduleImageGenerator {
    suspend fun createSchedule(group: YasnoGroup, outageHourRanges: List<IntRange>): ByteArray
}
