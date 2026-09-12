package io.orangebuffalo.simpleaccounting.infra.oauth2.impl

import io.orangebuffalo.simpleaccounting.infra.oauth2.SavedAuthorizationRequest
import io.orangebuffalo.simpleaccounting.infra.oauth2.SavedAuthorizationRequestRepository
import org.springframework.scheduling.TaskScheduler
import org.springframework.stereotype.Repository
import java.time.Duration
import java.time.Instant

private val requestLifetime: Duration = Duration.ofDays(2)

@Repository
class InMemorySavedAuthorizationRequestRepository(
    private val expiryScheduler: TaskScheduler,
) : SavedAuthorizationRequestRepository {

    private val requestsByState = mutableMapOf<String, ExpiringRequest>()
    private val requestsByOwner = mutableMapOf<RequestOwner, ExpiringRequest>()
    private var nextRequestId = 0L

    @Synchronized
    override fun findByStateAndRemove(state: String): SavedAuthorizationRequest {
        val request = requestsByState.remove(state)
        request?.let { requestsByOwner.remove(it.owner, it) }
        return request
            ?.takeIf { it.expiresAt.isAfter(expiryScheduler.clock.instant()) }
            ?.request
            ?: throw IllegalStateException("State $state is not known")
    }

    @Synchronized
    override fun getOrCreate(
        ownerId: String,
        clientRegistrationId: String,
        requestFactory: () -> SavedAuthorizationRequest,
    ): SavedAuthorizationRequest {
        val now = expiryScheduler.clock.instant()
        val owner = RequestOwner(ownerId, clientRegistrationId)
        requestsByOwner[owner]
            ?.takeIf { it.expiresAt.isAfter(now) }
            ?.let { return it.request }

        requestsByOwner.remove(owner)?.let { requestsByState.remove(it.request.state, it) }
        val authorizationRequest = requestFactory()
        val expiringRequest = ExpiringRequest(
            id = nextRequestId++,
            owner = owner,
            request = authorizationRequest,
            expiresAt = now.plus(requestLifetime),
        )
        requestsByState[authorizationRequest.state] = expiringRequest
        requestsByOwner[owner] = expiringRequest
        expiryScheduler.schedule(
            { removeExpiredRequest(authorizationRequest.state, owner, expiringRequest.id) },
            expiringRequest.expiresAt,
        )
        return authorizationRequest
    }

    @Synchronized
    private fun removeExpiredRequest(state: String, owner: RequestOwner, requestId: Long) {
        val request = requestsByState[state]?.takeIf { it.id == requestId } ?: return
        requestsByState.remove(state, request)
        requestsByOwner.remove(owner, request)
    }

    private class ExpiringRequest(
        val id: Long,
        val owner: RequestOwner,
        val request: SavedAuthorizationRequest,
        val expiresAt: Instant,
    )

    private data class RequestOwner(
        val ownerId: String,
        val clientRegistrationId: String,
    )
}
