package io.orangebuffalo.simpleaccounting.tests.infra.database

import org.testcontainers.containers.PostgreSQLContainer
import java.nio.file.Files

object PostgresTestDatabase {
    private val temporaryDirectory = Files.createTempDirectory("sa-postgres-test-")
    val missingLegacyH2Path = temporaryDirectory.resolve("missing")

    val container: PostgreSQLContainer<Nothing> by lazy {
        PostgreSQLContainer<Nothing>("postgres:17-alpine").also {
            it.start()
            Runtime.getRuntime().addShutdownHook(Thread {
                it.stop()
                Files.deleteIfExists(temporaryDirectory)
            })
        }
    }
}
