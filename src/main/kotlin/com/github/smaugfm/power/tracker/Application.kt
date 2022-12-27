package com.github.smaugfm.power.tracker

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootApplication
@EnableJpaRepositories
class Application {
    @Bean
    fun scope(): CoroutineScope = AppCoroutineScope()

    @Bean
    fun start(scope: CoroutineScope): Job {
        return scope.launch {

        }
    }
}

class AppCoroutineScope : CoroutineScope by CoroutineScope(Dispatchers.Default)

suspend fun main(args: Array<String>) {
    val context = runApplication<Application>(*args)
    context.getBeansOfType(Job::class.java).values.joinAll()
}
