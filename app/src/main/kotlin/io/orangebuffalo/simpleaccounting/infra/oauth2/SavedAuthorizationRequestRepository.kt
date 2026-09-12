package io.orangebuffalo.simpleaccounting.infra.oauth2

/**
 * Manages authorization requests persistently.
 */
interface SavedAuthorizationRequestRepository {

    /**
     * Finds a request by the state token or fails if not found.
     * Removes the request from the storage if found.
     */
    fun findByStateAndRemove(state: String): SavedAuthorizationRequest

    /**
     * Returns the pending request for the user and client registration, or atomically creates one.
     */
    fun getOrCreate(
        ownerId: String,
        clientRegistrationId: String,
        requestFactory: () -> SavedAuthorizationRequest,
    ): SavedAuthorizationRequest
}
