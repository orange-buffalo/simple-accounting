package io.orangebuffalo.simpleaccounting.infra.oauth2

import io.orangebuffalo.simpleaccounting.business.users.PlatformUser
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.coroutines.ContinuationInterceptor
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

    @OptIn(DelicateCoroutinesApi::class)
    fun executeInSourceContext(clientRegistrationId: String, block: suspend () -> Unit) {
        if (this.clientRegistrationId == clientRegistrationId) {
            val sourceContext = context
                .minusKey(ContinuationInterceptor)
                .minusKey(Job)
            runBlocking {
                withContext(sourceContext) {
                    block()
                }
            }
        }
    }
}
