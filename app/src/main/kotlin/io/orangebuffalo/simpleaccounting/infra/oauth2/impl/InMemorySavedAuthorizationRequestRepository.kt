package io.orangebuffalo.simpleaccounting.infra.oauth2.impl

import io.orangebuffalo.simpleaccounting.infra.oauth2.SavedAuthorizationRequest
import io.orangebuffalo.simpleaccounting.infra.oauth2.SavedAuthorizationRequestRepository
import org.springframework.stereotype.Repository
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

private val requestLifetime: Duration = Duration.ofDays(2)

@Repository
class InMemorySavedAuthorizationRequestRepository : SavedAuthorizationRequestRepository {

    private val requests = ConcurrentHashMap<String, ExpiringRequest>()

    override fun findByStateAndRemove(state: String): SavedAuthorizationRequest {
        val request = requests.remove(state)
        return request
            ?.takeIf { it.expiresAt.isAfter(Instant.now()) }
            ?.request
            ?: throw IllegalStateException("State $state is not known")
    }

    override fun save(authorizationRequest: SavedAuthorizationRequest) {
        removeExpiredRequests()
        requests[authorizationRequest.state] = ExpiringRequest(
            request = authorizationRequest,
            expiresAt = Instant.now().plus(requestLifetime),
        )
    }

    private fun removeExpiredRequests() {
        val now = Instant.now()
        requests.entries.removeIf { !it.value.expiresAt.isAfter(now) }
    }

    private data class ExpiringRequest(val request: SavedAuthorizationRequest, val expiresAt: Instant)
}
