package io.orangebuffalo.simpleaccounting.infra

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@ConfigurationProperties("simpleaccounting")
@Component
data class SimpleAccountingProperties(
    var publicUrl: String = "",
)
