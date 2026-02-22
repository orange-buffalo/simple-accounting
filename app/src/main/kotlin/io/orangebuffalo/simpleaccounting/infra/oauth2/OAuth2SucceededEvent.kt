package io.orangebuffalo.simpleaccounting.infra.oauth2

import io.orangebuffalo.simpleaccounting.business.security.runAs
import io.orangebuffalo.simpleaccounting.business.security.toSecurityPrincipal
import io.orangebuffalo.simpleaccounting.business.users.PlatformUser
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
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
    fun executeInSourceContext(clientRegistrationId: String, block: suspend () -> Unit) {
        if (this.clientRegistrationId == clientRegistrationId) {
            runBlocking {
                withContext(context) {
                    runAs(user.toSecurityPrincipal(), block)
                }
            }
        }
    }
}
