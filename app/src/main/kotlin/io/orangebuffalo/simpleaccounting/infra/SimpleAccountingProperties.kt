package io.orangebuffalo.simpleaccounting.infra

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

/**
 * Root Simple Accounting application configuration.
 */
@ConfigurationProperties("sa")
@Component
data class SimpleAccountingProperties(
    /**
     * Public URL used by clients to access the application.
     */
    var publicUrl: String = "",
)
