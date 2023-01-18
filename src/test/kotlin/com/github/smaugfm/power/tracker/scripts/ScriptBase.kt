package com.github.smaugfm.power.tracker.scripts

import com.github.smaugfm.power.tracker.NoLiquibaseTestBase
import com.github.smaugfm.power.tracker.events.EventsService
import com.github.smaugfm.power.tracker.interaction.UserInteractionService
import com.github.smaugfm.power.tracker.persistence.ConfigsRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestPropertySource

@TestPropertySource(
    properties = [
        "spring.r2dbc.url=r2dbc:postgresql://localhost:5432/postgres",
        "spring.r2dbc.username=postgres",
    ]
)
class ScriptBase : NoLiquibaseTestBase() {

    @Autowired
    protected lateinit var eventsService: EventsService

    @Autowired
    protected lateinit var configsRepository: ConfigsRepository

    @Autowired
    protected lateinit var userInteraction: UserInteractionService
}
