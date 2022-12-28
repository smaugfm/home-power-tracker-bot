package com.github.smaugfm.power.tracker.interaction

import com.github.smaugfm.power.tracker.dto.Event
import com.github.smaugfm.power.tracker.dto.EventId
import kotlinx.coroutines.flow.Flow
import org.springframework.stereotype.Service

@Service
class UserInteractionServiceImpl : UserInteractionService {
    override suspend fun postEvent(event: Event) {
        TODO("Not yet implemented")
    }

    override suspend fun updateEvent(event: Event) {
        TODO("Not yet implemented")
    }

    override fun deletionFlow(): Flow<EventId> {
        TODO("Not yet implemented")
    }

    override fun exportFlow(): Flow<(events: Flow<Event>) -> Unit> {
        TODO("Not yet implemented")
    }
}
