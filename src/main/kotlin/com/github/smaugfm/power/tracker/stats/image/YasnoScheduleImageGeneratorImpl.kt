package com.github.smaugfm.power.tracker.stats.image

import com.github.smaugfm.power.tracker.ScheduleImageCreateRequest
import com.github.smaugfm.power.tracker.YasnoGroup
import com.github.smaugfm.power.tracker.getResourceAsText
import com.github.smaugfm.power.tracker.spring.LaunchCoroutineBean
import com.microsoft.playwright.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.consumeAsFlow
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private val log = KotlinLogging.logger { }

@OptIn(DelicateCoroutinesApi::class)
@Service
class YasnoScheduleImageGeneratorImpl : LaunchCoroutineBean, YasnoScheduleImageGenerator {
    private val singleThreadedContext = newSingleThreadContext("yasno")

    private val channelDeferred = CompletableDeferred<SendChannel<ScheduleImageCreateRequest>>()

    override suspend fun createSchedule(
        group: YasnoGroup,
        mondayDate: LocalDate,
        outageHourRanges: List<IntRange>
    ): ByteArray {
        val deferredResult = CompletableDeferred<ByteArray>()
        channelDeferred.await()
            .send(ScheduleImageCreateRequest(group, outageHourRanges, mondayDate, deferredResult))
        return deferredResult.await()
    }

    override suspend fun launch(scope: CoroutineScope) {
        withContext(singleThreadedContext) {
            Playwright.create().use { playwright ->
                playwright.chromium().launch().use { browser ->
                    log.info { "Headless Chromium created" }
                    val context = browser.newContext(
                        Browser.NewContextOptions().setDeviceScaleFactor(4.0)
                    )

                    val ch = Channel<ScheduleImageCreateRequest>()
                    channelDeferred.complete(ch)
                    ch.consumeAsFlow().collect { request ->
                        log.info { "Screenshot request received: $request" }
                        val (group, ranges, mondayDate, future) = request
                        try {
                            val bytes = getScreenshotBytes(context, mondayDate, ranges, group)
                            future.complete(bytes)
                        } catch (e: Throwable) {
                            log.error(e) { "Error making Chromium screenshot" }
                            future.completeExceptionally(e)
                        }
                    }
                }
            }
        }
    }

    private fun getScreenshotBytes(
        context: BrowserContext,
        mondayDate: LocalDate,
        outageHourRanges: List<IntRange>,
        group: YasnoGroup
    ): ByteArray {
        val num = when (group) {
            YasnoGroup.Group1 -> 1
            YasnoGroup.Group2 -> 2
        }

        val html = getResourceAsText("yasno-web/schedule-group$num.html")
        val css = getResourceAsText("yasno-web/app.css")

        context.newPage().use { page ->
            page.setViewportSize(VIEWPORT_WIDTH, VIEWPORT_HEIGHT)
            page.setContent(html)
            page.addStyleTag(Page.AddStyleTagOptions().setContent(css))

            setOutageHours(page, outageHourRanges)
            setDates(page, mondayDate)
            setTodayActive(page, mondayDate)

            return page.screenshot()
        }
    }

    private fun setTodayActive(page: Page, mondayDate: LocalDate) {
        val todayDayOfWeek = Duration.between(
            mondayDate.atStartOfDay(),
            ZonedDateTime
                .now()
                .toLocalDate()
                .atStartOfDay()
        ).toDays()
        if (todayDayOfWeek >= 7)
            return

        val element = queryDayOfWeek(page, todayDayOfWeek.toInt())
        element.evaluate("node => node.classList.add('active')")
    }

    private fun setDates(page: Page, mondayDate: LocalDate) {
        (0..6).forEach { dayOfWeek ->
            val date = mondayDate.plusDays(dayOfWeek.toLong()).format(
                DateTimeFormatter.ofPattern("dd.MM")
            )
            val element = queryDayOfWeek(page, dayOfWeek)
            element.evaluate("node => { node.innerHTML = node.innerText + '<br>$date'}")
        }
    }

    private fun queryDayOfWeek(page: Page, dayOfWeek: Int): ElementHandle {
        val col = SCHEDULE_HTML_FIRST_DAY_OF_WEEK_INDEX + (dayOfWeek * SCHEDULE_HTML_DAY_COL_MULTIPLIER)
        return page.querySelector(scheduleHtmlColSelector(col))
    }

    private fun setOutageHours(page: Page, outageHourRanges: List<IntRange>) {
        outageHourRanges.forEach { range ->
            range.forEach { i ->
                val day = i / 24
                val hour = i - day * 24

                val col = SCHEDULE_HTML_FIRST_HOUR_INDEX + (day * SCHEDULE_HTML_DAY_COL_MULTIPLIER) + hour
                val element = page.querySelector(scheduleHtmlColSelector(col))
                element.evaluate("node => node.classList.add('$SCHEDULE_HTML_COL_OUTAGE_CLASS_NAME')")
            }
        }
    }

    companion object {
        private fun scheduleHtmlColSelector(col: Int) =
            "body > div > div > div > div > div > div:nth-child($col)"

        private const val SCHEDULE_HTML_COL_OUTAGE_CLASS_NAME = "FACTUAL_OUTAGE"
        private const val SCHEDULE_HTML_FIRST_HOUR_INDEX = 27
        private const val SCHEDULE_HTML_FIRST_DAY_OF_WEEK_INDEX = 26
        private const val SCHEDULE_HTML_DAY_COL_MULTIPLIER = 25
        private const val VIEWPORT_WIDTH = 757
        private const val VIEWPORT_HEIGHT = 1100
    }
}
