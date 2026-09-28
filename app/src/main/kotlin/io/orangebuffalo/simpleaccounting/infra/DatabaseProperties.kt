package io.orangebuffalo.simpleaccounting.infra

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import java.nio.file.Path

/**
 * Database credentials used by the application datasource configuration.
 */
@ConfigurationProperties("sa.database")
@Component
data class DatabaseProperties(
    /**
     * Database user name.
     */
    var username: String = "sa",

    /**
     * Database user password.
     */
    var password: String = "",
    var host: String = "localhost",
    var port: Int = 5432,
    var name: String = "simple-accounting",
    var legacyH2Path: Path = Path.of("/data/db/simple-accounting"),
    var legacyH2Username: String? = null,
    var legacyH2Password: String? = null,
)
