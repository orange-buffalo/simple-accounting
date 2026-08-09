package io.orangebuffalo.simpleaccounting.infra.oauth2

import io.orangebuffalo.simpleaccounting.business.security.runAs
import io.orangebuffalo.simpleaccounting.business.security.toSecurityPrincipal
import io.orangebuffalo.simpleaccounting.business.users.PlatformUser

/**
 * Event indicating a failed OAuth2 authorization request
 */
data class OAuth2FailedEvent(
    val user: PlatformUser,
    val clientRegistrationId: String,
) {

    fun executeInSourceContext(clientRegistrationId: String, block: () -> Unit) {
        if (this.clientRegistrationId == clientRegistrationId) {
            runAs(user.toSecurityPrincipal(), block)
        }
    }
}
