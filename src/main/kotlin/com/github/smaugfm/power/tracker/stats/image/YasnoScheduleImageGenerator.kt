package com.github.smaugfm.power.tracker.stats.image

import com.github.smaugfm.power.tracker.YasnoGroup
import java.time.LocalDate

interface YasnoScheduleImageGenerator {
    suspend fun createSchedule(
        group: YasnoGroup,
        mondayDate: LocalDate,
        outageHourRanges: List<IntRange>): ByteArray
}
