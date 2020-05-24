package io.orangebuffalo.simpleaccounting.services.integration.oauth2.impl

import io.orangebuffalo.simpleaccounting.services.integration.oauth2.OAuth2AuthorizationRequest
import io.orangebuffalo.simpleaccounting.services.integration.oauth2.Oauth2AuthorizationRequestRepository
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.springframework.stereotype.Repository
import java.time.Duration

@Repository
class InMemoryOauth2AuthorizationRequestRepository : Oauth2AuthorizationRequestRepository {

    private val requests = mutableMapOf<String, OAuth2AuthorizationRequest>()
    private val mutex = Mutex()

    override suspend fun findByStateAndRemove(state: String): OAuth2AuthorizationRequest {
        val request = mutex.withLock { requests.remove(state) }
        return request ?: throw IllegalStateException("State $state is not known")
    }

    override suspend fun save(authorizationRequest: OAuth2AuthorizationRequest) {
        mutex.withLock {
            requests.put(authorizationRequest.state, authorizationRequest)
        }
        scheduleRequestCleanup(authorizationRequest.state)
    }

    private fun scheduleRequestCleanup(state: String) {
        GlobalScope.launch {
            delay(Duration.ofDays(2).toMillis())
            mutex.withLock { requests.remove(state) }
        }
    }
}
