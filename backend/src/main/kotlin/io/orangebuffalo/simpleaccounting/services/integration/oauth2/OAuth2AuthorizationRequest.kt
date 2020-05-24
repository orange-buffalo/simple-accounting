package io.orangebuffalo.simpleaccounting.services.integration.oauth2

class OAuth2AuthorizationRequest(
    val ownerId: Long,
    val state: String,
    val clientRegistrationId: String
)
