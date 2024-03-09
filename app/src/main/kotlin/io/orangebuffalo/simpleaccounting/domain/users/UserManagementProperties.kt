package io.orangebuffalo.simpleaccounting.domain.users

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

/**
 * Configuration properties for user management.
 */
@ConfigurationProperties("simpleaccounting.user-management")
@Component
data class UserManagementProperties(
    /**
     * User activation properties.
     */
    var activation: Activation = Activation(),
) {

    data class Activation(
        /**
         * The time-to-live for user activation tokens in hours.
         */
        var tokenTtlInHours: Int = 72,
    )
}
