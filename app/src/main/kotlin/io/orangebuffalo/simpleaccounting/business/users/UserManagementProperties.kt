package io.orangebuffalo.simpleaccounting.business.users

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

        /**
         * The delay in milliseconds to apply when a token verification is requested, to
         * protect against brute force attacks.
         */
        var tokenVerificationBruteForceDelayInMs: Long = 1000,
    )
}
