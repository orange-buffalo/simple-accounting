package io.orangebuffalo.simpleaccounting.tests.infra.thirdparty

import org.springframework.context.ConfigurableApplicationContext

object GoogleOAuthApiMocks {
    const val CLIENT_ID = "test-google-client-id"
    const val CLIENT_SECRET = "test-google-client-secret"
    private const val ISSUER_NAME = "google"

    private const val GOOGLE_OAUTH_MOCKS_ROOT_PATH = "/google-oauth"

    fun configProperties(applicationContext: ConfigurableApplicationContext): Array<String> {
        val issuerUrl = mockOAuthServer.issuerUrl(ISSUER_NAME)
        return arrayOf(
            "spring.security.oauth2.client.provider.google.issuer-uri=$issuerUrl",
            "spring.security.oauth2.client.registration.google-drive.scope=test-scope,openid",
            "spring.security.oauth2.client.registration.google-drive.client-id=$CLIENT_ID",
            "spring.security.oauth2.client.registration.google-drive.client-secret=$CLIENT_SECRET",

//            "spring.security.oauth2.client.provider.google.authorization-uri=" +
//                    "http://localhost:${wireMockServer.port()}$GOOGLE_OAUTH_MOCKS_ROOT_PATH/authorize",
//            "spring.security.oauth2.client.provider.google.token-uri=" +
//                    "http://localhost:${wireMockServer.port()}$GOOGLE_OAUTH_MOCKS_ROOT_PATH/token",
//            "spring.security.oauth2.client.provider.google.user-info-uri=" +
//                    "http://localhost:${wireMockServer.port()}$GOOGLE_OAUTH_MOCKS_ROOT_PATH/userinfo",
//            "spring.security.oauth2.client.provider.google.jwk-set-uri=" +
//                    "http://localhost:${wireMockServer.port()}$GOOGLE_OAUTH_MOCKS_ROOT_PATH/jwks.json",
        )
    }
}
