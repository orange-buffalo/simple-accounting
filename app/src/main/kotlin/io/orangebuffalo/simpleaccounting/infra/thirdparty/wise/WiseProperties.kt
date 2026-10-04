package io.orangebuffalo.simpleaccounting.infra.thirdparty.wise

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

/** Configuration for the Wise API connection. */
@Component
@ConfigurationProperties("sa.wise")
data class WiseProperties(
    /** Wise API origin without an API version. Each endpoint chooses its supported version. */
    var apiBaseUrl: String = "https://api.wise.com",
)
