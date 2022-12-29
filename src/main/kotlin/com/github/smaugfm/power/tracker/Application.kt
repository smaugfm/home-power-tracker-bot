package com.github.smaugfm.power.tracker

import com.github.smaugfm.power.tracker.spring.CoroutinesLaunchAdapter
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories

@SpringBootApplication
@EnableR2dbcRepositories
@EnableR2dbcAuditing(dateTimeProviderRef = "zonedDateTimeProvider")
class Application

suspend fun main(args: Array<String>) {
    val context = runApplication<Application>(*args)
    supervisorScope {
        context.getBeansOfType(CoroutinesLaunchAdapter::class.java)
            .values.map {
                launch { it.launch() }
            }.joinAll()
    }
}
