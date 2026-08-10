package io.orangebuffalo.simpleaccounting.infra.oauth2.impl

import io.orangebuffalo.simpleaccounting.infra.oauth2.SavedAuthorizationRequest
import io.orangebuffalo.simpleaccounting.infra.oauth2.SavedAuthorizationRequestRepository
import org.springframework.scheduling.TaskScheduler
import org.springframework.stereotype.Repository
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

private val requestLifetime: Duration = Duration.ofDays(2)

@Repository
class InMemorySavedAuthorizationRequestRepository(
    private val expiryScheduler: TaskScheduler,
) : SavedAuthorizationRequestRepository {

    private val requests = ConcurrentHashMap<String, ExpiringRequest>()

    override fun findByStateAndRemove(state: String): SavedAuthorizationRequest {
        val request = requests.remove(state)
        return request
            ?.takeIf { it.expiresAt.isAfter(expiryScheduler.clock.instant()) }
            ?.request
            ?: throw IllegalStateException("State $state is not known")
    }

    override fun save(authorizationRequest: SavedAuthorizationRequest) {
        val expiringRequest = ExpiringRequest(
            request = authorizationRequest,
            expiresAt = expiryScheduler.clock.instant().plus(requestLifetime),
        )
        requests[authorizationRequest.state] = expiringRequest
        expiryScheduler.schedule(
            { requests.remove(authorizationRequest.state, expiringRequest) },
            expiringRequest.expiresAt,
        )
    }

    private class ExpiringRequest(
        val request: SavedAuthorizationRequest,
        val expiresAt: Instant,
    )
}
