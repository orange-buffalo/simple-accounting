package io.orangebuffalo.simpleaccounting.services.integration.oauth2

/**
 * Manages authorization requests persistently.
 */
interface Oauth2AuthorizationRequestRepository {
    suspend fun findByStateAndRemove(state: String): OAuth2AuthorizationRequest
    suspend fun save(authorizationRequest: OAuth2AuthorizationRequest)
}
