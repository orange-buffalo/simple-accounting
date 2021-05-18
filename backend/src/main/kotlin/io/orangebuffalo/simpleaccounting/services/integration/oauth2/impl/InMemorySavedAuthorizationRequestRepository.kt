package io.orangebuffalo.simpleaccounting.services.integration.oauth2.impl

import io.orangebuffalo.simpleaccounting.services.integration.oauth2.SavedAuthorizationRequest
import io.orangebuffalo.simpleaccounting.services.integration.oauth2.SavedAuthorizationRequestRepository
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.springframework.stereotype.Repository
import java.time.Duration

@Repository
class InMemorySavedAuthorizationRequestRepository : SavedAuthorizationRequestRepository {

    private val requests = mutableMapOf<String, SavedAuthorizationRequest>()
    private val mutex = Mutex()

    override suspend fun findByStateAndRemove(state: String): SavedAuthorizationRequest {
        val request = mutex.withLock { requests.remove(state) }
        return request ?: throw IllegalStateException("State $state is not known")
    }

    override suspend fun save(authorizationRequest: SavedAuthorizationRequest) {
        mutex.withLock {
            requests.put(authorizationRequest.state, authorizationRequest)
        }
        scheduleRequestCleanup(authorizationRequest.state)
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun scheduleRequestCleanup(state: String) {
        GlobalScope.launch {
            delay(Duration.ofDays(2).toMillis())
            mutex.withLock { requests.remove(state) }
        }
    }
}
