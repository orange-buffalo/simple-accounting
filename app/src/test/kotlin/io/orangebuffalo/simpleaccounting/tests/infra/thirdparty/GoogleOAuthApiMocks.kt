package io.orangebuffalo.simpleaccounting.tests.infra.thirdparty

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

object GoogleOAuthApiMocks {
    private const val CLIENT_ID = "test-google-client-id"
    private const val CLIENT_SECRET = "secret"
    private const val ISSUER_NAME = "google"

    fun configProperties(): Array<String> {
        val issuerUrl = OAuthMocks.mockOAuthServer.issuerUrl(ISSUER_NAME)
        return arrayOf(
            "spring.security.oauth2.client.provider.google.issuer-uri=$issuerUrl",
            "spring.security.oauth2.client.registration.google-drive.scope=test-scope,openid",
            "spring.security.oauth2.client.registration.google-drive.client-id=$CLIENT_ID",
            "spring.security.oauth2.client.registration.google-drive.client-secret=$CLIENT_SECRET",
        )
    }

    /**
     * Configures this provider to return a valid access token. Returns this token
     * for assertions / stubbing.
     */
    fun enqueueAccessToken() = OAuthMocks.enqueueAccessToken(
        issuerId = ISSUER_NAME,
        clientId = CLIENT_ID,
    )

    fun recordedRequests(): List<GoogleOAuthRecordedRequest> = OAuthMocks.recordedRequests(ISSUER_NAME)
        .map { oauthRequest ->
            when (oauthRequest) {
                is OAuthRecordedRequest.Authorize -> {
                    withClue("Authorize request should use valid data") {
                        oauthRequest.clientId.shouldBe(CLIENT_ID)
                        oauthRequest.scopes.shouldContainExactlyInAnyOrder("test-scope", "openid")
                    }
                    GoogleOAuthRecordedRequest.Authorize
                }

                is OAuthRecordedRequest.Token -> {
                    withClue("Token request should use valid data") {
                        oauthRequest.clientId.shouldBe(CLIENT_ID)
                        oauthRequest.clientSecret.shouldBe(CLIENT_SECRET)
                    }
                    GoogleOAuthRecordedRequest.Token
                }
            }
        }
}

sealed interface GoogleOAuthRecordedRequest {
    data object Authorize : GoogleOAuthRecordedRequest
    data object Token : GoogleOAuthRecordedRequest
}

