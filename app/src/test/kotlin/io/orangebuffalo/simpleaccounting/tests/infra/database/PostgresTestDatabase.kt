package io.orangebuffalo.simpleaccounting.tests.infra.database

import org.testcontainers.containers.PostgreSQLContainer

object PostgresTestDatabase {
    val container: PostgreSQLContainer<Nothing> by lazy {
        PostgreSQLContainer<Nothing>("postgres:17-alpine").also {
            it.start()
            Runtime.getRuntime().addShutdownHook(Thread {
                it.stop()
            })
        }
    }
}
