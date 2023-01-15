package com.github.smaugfm.power.tracker.stats

import com.github.smaugfm.power.tracker.Event
import com.github.smaugfm.power.tracker.getResourceAsText
import com.microsoft.playwright.Browser
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Service
import java.nio.file.Paths

@Order(3)
@Service
class ScreenshotSummaryStatsService : StatsService {
    override suspend fun calculate(event: Event): List<EventStats> {
        withContext(Dispatchers.IO) {
            Playwright.create().use { playwright: Playwright ->
                playwright.chromium().launch().use { browser ->
                    val context = browser.newContext(
                        Browser.NewContextOptions().setDeviceScaleFactor(4.0)
                    )
                    val page = context.newPage()
                    val html = getResourceAsText("yasno-web/schedule-group1.html")
                    val css = getResourceAsText("yasno-web/app.css")
                    page.setViewportSize(757, 1100)
                    page.setContent(html)
                    page.addStyleTag(
                        Page.AddStyleTagOptions().setContent(
                            css
                        )
                    )
                    page.screenshot(
                        Page.ScreenshotOptions()
                            .setQuality(95)
                            .setPath(Paths.get("vasa.jpg"))
                    )
                }
            }
        }
        return listOf()
    }
}
