package io.orangebuffalo.simpleaccounting.tests.infra.thirdparty

import io.orangebuffalo.simpleaccounting.business.documents.storage.gdrive.OAUTH2_CLIENT_REGISTRATION_ID

private const val CLIENT_ID = "test-google-client-id"
private const val CLIENT_SECRET = "secret"
private const val ISSUER_NAME = "google"

object GoogleOAuthMocks : OAuthMocksProvider by OAuthMocks.provider(
    issuerId = ISSUER_NAME,
    clientId = CLIENT_ID,
    clientSecret = CLIENT_SECRET,
    scopes = listOf("test-scope", "openid"),
    clientRegistrationId = OAUTH2_CLIENT_REGISTRATION_ID,
) {
    fun configProperties(): Array<String> = arrayOf(
        "spring.security.oauth2.client.provider.google.issuer-uri=${OAuthMocks.issuerUrl(ISSUER_NAME)}",
        "spring.security.oauth2.client.provider.google.authorization-uri=${OAuthMocks.authorizationUrl(ISSUER_NAME)}",
        "spring.security.oauth2.client.registration.google-drive.scope=test-scope,openid",
        "spring.security.oauth2.client.registration.google-drive.client-id=$CLIENT_ID",
        "spring.security.oauth2.client.registration.google-drive.client-secret=$CLIENT_SECRET",
    )
}
