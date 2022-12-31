package com.github.smaugfm.power.tracker

import org.h2.tools.DeleteDbFiles
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
@SpringBootTest(classes = [Application::class])
class TestBase

class RepositoryTestBase : TestBase() {

    @Autowired
    protected lateinit var db: DatabaseClient

    @BeforeEach
    fun beforeEach() {
        db.sql("set REFERENTIAL_INTEGRITY = false").then().block()
        db.sql("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES  where TABLE_SCHEMA='PUBLIC'")
            .map { t, _ -> t.get(0, String::class.java) }
            .all()
            .filter { !(it?.startsWith("DATABASECHANELOG") ?: true) }
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

    companion object {
        @BeforeAll
        @JvmStatic
        fun deleteH2() {
            DeleteDbFiles.main()
        }
    }
}

