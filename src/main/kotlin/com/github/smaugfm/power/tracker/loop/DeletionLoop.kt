package com.github.smaugfm.power.tracker.loop

import com.github.smaugfm.power.tracker.events.EventsService
import com.github.smaugfm.power.tracker.interaction.UserInteractionService
import com.github.smaugfm.power.tracker.spring.LaunchCoroutineBean
import kotlinx.coroutines.CoroutineScope
import mu.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger { }

@Component
class DeletionLoop(
    private val userInteraction: UserInteractionService,
    private val events: EventsService,
) : LaunchCoroutineBean {
    override suspend fun launch(scope: CoroutineScope) {
        userInteraction.deletionFlow().collect { deletionRequest ->
            val event = events.getEvent(deletionRequest.eventId)
            if (event == null) {
                log.warn { "Missing event for request=$deletionRequest" }
                return@collect
            }
            userInteraction.deleteForEvent(event)
            events.deleteEvent(deletionRequest.eventId)

            events
                .getEventsAfter(event.configId, event.time)
                .collect(userInteraction::updateForEvent)
        }
    }
}
