package io.orangebuffalo.simpleaccounting.infra

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

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
)
