package com.github.smaugfm.power.tracker

import com.github.smaugfm.power.tracker.spring.LaunchCoroutineBean
import com.github.smaugfm.power.tracker.spring.LoopProperties
import com.github.smaugfm.power.tracker.spring.NetworkStabilityProperties
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories

@SpringBootApplication
@EnableR2dbcRepositories
@EnableR2dbcAuditing
@EnableConfigurationProperties(
    value =
    [LoopProperties::class, NetworkStabilityProperties::class]
)
class Application {
    companion object {
        suspend fun run(context: ConfigurableApplicationContext) {
            supervisorScope {
                context.getBeansOfType(LaunchCoroutineBean::class.java)
                    .values.map {
                        launch { it.launch(this) }
                    }.joinAll()
            }
        }
    }
}

suspend fun main(args: Array<String>) {
    Application.run(runApplication<Application>(*args))
}
