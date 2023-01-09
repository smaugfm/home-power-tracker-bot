package com.github.smaugfm.power.tracker.loop

import com.github.smaugfm.power.tracker.events.EventsService
import com.github.smaugfm.power.tracker.interaction.UserInteractionService
import com.github.smaugfm.power.tracker.spring.LaunchCoroutineBean
import kotlinx.coroutines.CoroutineScope
import org.springframework.stereotype.Component

@Component
class ExportLoop(
    private val userInteraction: UserInteractionService,
    private val events: EventsService,
) : LaunchCoroutineBean {
    override suspend fun launch(scope: CoroutineScope) {
        userInteraction.exportFlow().collect { configId ->
            userInteraction.exportEvents(configId, events.findAllEvents(configId))
        }
    }
}
