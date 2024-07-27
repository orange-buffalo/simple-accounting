package io.orangebuffalo.simpleaccounting.infra.oauth2.impl

import io.orangebuffalo.simpleaccounting.infra.oauth2.SavedAuthorizationRequest
import io.orangebuffalo.simpleaccounting.infra.oauth2.SavedAuthorizationRequestRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.springframework.stereotype.Repository
import java.time.Duration
import jakarta.annotation.PreDestroy

@Repository
class InMemorySavedAuthorizationRequestRepository : SavedAuthorizationRequestRepository {

    private val requests = mutableMapOf<String, SavedAuthorizationRequest>()
    private val mutex = Mutex()
    private val scope = CoroutineScope(Dispatchers.Default)

    @PreDestroy
    fun cancelAsyncJobs() {
        scope.cancel()
    }

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

    private fun scheduleRequestCleanup(state: String) {
        scope.launch {
            delay(Duration.ofDays(2).toMillis())
            mutex.withLock { requests.remove(state) }
        }
    }
}
