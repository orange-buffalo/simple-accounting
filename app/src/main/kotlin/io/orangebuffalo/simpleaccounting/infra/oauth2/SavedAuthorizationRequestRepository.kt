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
     * Saves the request to the storage.
     */
    fun save(authorizationRequest: SavedAuthorizationRequest)
}
