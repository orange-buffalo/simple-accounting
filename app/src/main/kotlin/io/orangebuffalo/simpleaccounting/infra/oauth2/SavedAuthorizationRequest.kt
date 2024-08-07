package io.orangebuffalo.simpleaccounting.infra.oauth2

import io.orangebuffalo.simpleaccounting.business.users.PlatformUser
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest

/**
 * Wraps [OAuth2AuthorizationRequest] to be saved between user requests.
 */
class SavedAuthorizationRequest(
    val owner: PlatformUser,
    val state: String,
    val clientRegistrationId: String,
    val request: OAuth2AuthorizationRequest
)
