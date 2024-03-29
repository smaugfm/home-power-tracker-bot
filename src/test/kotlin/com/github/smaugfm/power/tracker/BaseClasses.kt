package com.github.smaugfm.power.tracker

import com.github.smaugfm.power.tracker.persistence.ConfigEntity
import com.github.smaugfm.power.tracker.persistence.ConfigsRepository
import com.github.smaugfm.power.tracker.persistence.EventsRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
@SpringBootTest(classes = [Application::class])
@ExtendWith(DeleteDbFilesExtension::class)
class TestBase

@EnableAutoConfiguration(exclude = [LiquibaseAutoConfiguration::class])
class NoLiquibaseTestBase : TestBase()

class RepositoryTestBase : TestBase() {
    @Autowired
    protected lateinit var configRepository: ConfigsRepository

    @Autowired
    protected lateinit var eventsRepository: EventsRepository

    @Autowired
    protected lateinit var db: DatabaseClient

    @BeforeEach
    fun beforeEach() {
        db.sql("set REFERENTIAL_INTEGRITY = false").then().block()
        db.sql("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES  where TABLE_SCHEMA='PUBLIC'")
            .map { t, _ -> t.get(0, String::class.java)!! }
            .all()
            .filter { tableName ->
                !tableName.lowercase().startsWith("databasechangelog")
            }
            .flatMap {
                db.sql("TRUNCATE TABLE $it")
                    .then()
            }.then()
            .block()
        db.sql("SELECT SEQUENCE_NAME FROM INFORMATION_SCHEMA.SEQUENCES WHERE SEQUENCE_SCHEMA='PUBLIC'")
            .map { t, _ -> t.get(0, String::class.java) }
            .all()
            .flatMap {
                db.sql("ALTER SEQUENCE $it RESTART WITH 1")
                    .then()
            }
            .then()
            .block()
        db.sql("set REFERENTIAL_INTEGRITY = true").then().block()
    }

    protected fun saveConfig2(): ConfigEntity =
        configRepository.save(
            ConfigEntity(
                "other.com",
                8080,
            )
        ).block()!!

    protected fun saveConfig1(): ConfigEntity =
        configRepository.save(
            ConfigEntity(
                "vasa.com",
                8080,
            )
        ).block()!!

    protected fun saveConfigNoPort(): ConfigEntity =
        configRepository.save(
            ConfigEntity(
                "vasa.com",
                null
            )
        ).block()!!
}

