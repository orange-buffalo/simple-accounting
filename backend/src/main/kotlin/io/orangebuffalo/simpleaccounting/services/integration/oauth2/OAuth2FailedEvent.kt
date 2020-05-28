package io.orangebuffalo.simpleaccounting.services.integration.oauth2

import io.orangebuffalo.simpleaccounting.services.persistence.entities.PlatformUser
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * Event indicating a failed OAuth2 authorization request
 */
data class OAuth2FailedEvent(
    val user: PlatformUser,
    val clientRegistrationId: String,
    // required to preserve the caller's context, like WebExchange
    val context: CoroutineContext
) {

    fun launchIfClientMatches(clientRegistrationId: String, block: suspend () -> Unit) {
        if (this.clientRegistrationId == clientRegistrationId) {
            GlobalScope.launch(context) { block() }
        }
    }
}
