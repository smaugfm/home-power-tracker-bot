package com.github.smaugfm.power.tracker.events

interface EventsService {
    suspend fun addNewEvent(event: NewEvent): Event
    suspend fun deleteEvent(eventId: EventId)
    suspend fun updateEvent(event: Event)
}
