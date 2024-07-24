package io.orangebuffalo.simpleaccounting.services.integration.oauth2

import io.orangebuffalo.simpleaccounting.domain.users.PlatformUser
import io.orangebuffalo.simpleaccounting.domain.security.runAs
import io.orangebuffalo.simpleaccounting.domain.security.toSecurityPrincipal
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * Event indicating a successful OAuth2 authorization request.
 */
data class OAuth2SucceededEvent(
    val user: PlatformUser,
    val clientRegistrationId: String,
    // required to preserve the caller's context, like WebExchange
    val context: CoroutineContext
) {

    @OptIn(DelicateCoroutinesApi::class)
    fun launchIfClientMatches(clientRegistrationId: String, block: suspend () -> Unit): Job? =
        if (this.clientRegistrationId == clientRegistrationId) {
            GlobalScope.launch(context) {
                runAs(user.toSecurityPrincipal(), block)
            }
        } else null
}
