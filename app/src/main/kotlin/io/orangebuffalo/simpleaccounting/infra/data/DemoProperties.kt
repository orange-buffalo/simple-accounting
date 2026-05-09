package io.orangebuffalo.simpleaccounting.infra.data

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

/**
 * Demo data generation configuration.
 */
@ConfigurationProperties("sa.demo")
@Component
data class DemoProperties(
    /**
     * Whether demo data should be generated during application startup.
     */
    var enabled: Boolean = false,
)
